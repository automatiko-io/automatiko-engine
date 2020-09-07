package io.automatik.engine.addons.jobs.management.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.Model;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.Processes;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.services.uow.UnitOfWorkExecutor;
import io.automatik.engine.workflow.Sig;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@IfBuildProperty(name = "quarkus.automatik.jobs.type", stringValue = "filesystem")
@ApplicationScoped
public class FileSystemBasedJobService implements JobsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemBasedJobService.class);
	private static final String TRIGGER = "timer";

	private String storage;

	protected final UnitOfWorkManager unitOfWorkManager;

	protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();
	protected final ScheduledThreadPoolExecutor scheduler;

	protected ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();

	@Inject
	public FileSystemBasedJobService(@ConfigProperty(name = "quarkus.automatik.jobs.filesystem.path") String storage,
			@ConfigProperty(name = "quarkus.automatik.jobs.filesystem.threads", defaultValue = "1") int threads,
			Processes processes, Application application) {
		this.storage = storage;
		processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

		this.unitOfWorkManager = application.unitOfWorkManager();

		this.scheduler = new ScheduledThreadPoolExecutor(threads);
	}

	public void scheduleOnLoad(@Observes StartupEvent event) {
		Path start = Paths.get(storage);

		try {
			Files.createDirectories(start);
			Files.newDirectoryStream(start).forEach(this::loadAndSchedule);
		} catch (IOException e) {
			LOGGER.warn("Unable to load stored scheduled jobs", e);
		}
	}

	@Override
	public String scheduleProcessJob(ProcessJobDescription description) {
		LOGGER.debug("ScheduleProcessJob: {}", description);
		ScheduledFuture<?> future = null;
		ScheduledJob scheduledJob = null;
		if (description.expirationTime().repeatInterval() != null) {
			future = scheduler.scheduleAtFixedRate(repeatableProcessJobByDescription(description),
					calculateDelay(description.expirationTime().get()), description.expirationTime().repeatInterval(),
					TimeUnit.MILLISECONDS);

			scheduledJob = new ScheduledJob(description.id(),
					description.processId() + version(description.processVersion()), false,
					description.expirationTime().repeatLimit(), description.expirationTime().repeatInterval(),
					description.expirationTime().get());
		} else {
			future = scheduler.schedule(processJobByDescription(description),
					calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);

			scheduledJob = new ScheduledJob(description.id(), description.processId(), true, -1, null,
					description.expirationTime().get());
		}
		scheduledJobs.put(description.id(), future);
		storeScheduledJob(scheduledJob);
		return description.id();
	}

	@Override
	public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {
		ScheduledFuture<?> future = null;
		ScheduledJob scheduledJob = null;
		if (description.expirationTime().repeatInterval() != null) {
			future = scheduler.scheduleAtFixedRate(
					new SignalProcessInstanceOnExpiredTimer(description.id(),
							description.processId() + version(description.processVersion()),
							description.processInstanceId(), false, description.expirationTime().repeatLimit()),
					calculateDelay(description.expirationTime().get()), description.expirationTime().repeatInterval(),
					TimeUnit.MILLISECONDS);

			scheduledJob = new ScheduledJob(description.id(),
					description.processId() + version(description.processVersion()), false,
					description.processInstanceId(), description.expirationTime().repeatLimit(),
					description.expirationTime().repeatInterval(), description.expirationTime().get());
		} else {
			future = scheduler.schedule(
					new SignalProcessInstanceOnExpiredTimer(description.id(),
							description.processId() + version(description.processVersion()),
							description.processInstanceId(), true, -1),
					calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);

			scheduledJob = new ScheduledJob(description.id(),
					description.processId() + version(description.processVersion()), true,
					description.processInstanceId(), -1, null, description.expirationTime().get());
		}
		scheduledJobs.put(description.id(), future);
		storeScheduledJob(scheduledJob);
		return description.id();
	}

	@Override
	public boolean cancelJob(String id) {
		LOGGER.debug("Cancel Job: {}", id);
		if (scheduledJobs.containsKey(id)) {
			removeScheduledJob(id);
			return scheduledJobs.remove(id).cancel(true);
		}

		return false;
	}

	@Override
	public ZonedDateTime getScheduledTime(String id) {
		if (scheduledJobs.containsKey(id)) {
			ScheduledFuture<?> scheduled = scheduledJobs.get(id);

			long remainingTime = scheduled.getDelay(TimeUnit.MILLISECONDS);
			if (remainingTime > 0) {
				return ZonedDateTime.from(Instant.ofEpochMilli(System.currentTimeMillis() + remainingTime));
			}
		}

		return null;
	}

	public void shutown(@Observes ShutdownEvent event) {
		scheduledJobs.values().forEach(job -> job.cancel(false));
		scheduledJobs.clear();

		scheduler.shutdownNow();
	}

	protected void storeScheduledJob(ScheduledJob job) {
		Path path = Paths.get(storage, job.getId());

		try {
			Files.createDirectories(path.getParent());

			Files.write(path, mapper.writeValueAsBytes(job));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void loadAndSchedule(Path path) {

		try {
			if (Files.isDirectory(path) || Files.isHidden(path)) {
				return;
			}
			ScheduledJob job = mapper.readValue(Files.readAllBytes(path), ScheduledJob.class);
			ScheduledFuture<?> future = null;
			if (job.getProcessInstanceId() != null) {
				if (job.getReapeatInterval() != null) {
					future = scheduler.scheduleAtFixedRate(
							new SignalProcessInstanceOnExpiredTimer(job.getId(), job.getProcessId(),
									job.getProcessInstanceId(), false, job.getLimit()),
							calculateDelay(job.getFireTime()), job.getReapeatInterval(), TimeUnit.MILLISECONDS);

				} else {
					future = scheduler.schedule(
							new SignalProcessInstanceOnExpiredTimer(job.getId(), job.getProcessId(),
									job.getProcessInstanceId(), true, -1),
							calculateDelay(job.getFireTime()), TimeUnit.MILLISECONDS);
				}

			} else {
				if (job.getReapeatInterval() != null) {
					future = scheduler.scheduleAtFixedRate(
							new StartProcessOnExpiredTimer(job.getId(), job.getProcessId(), false, job.getLimit()),
							calculateDelay(job.getFireTime()), job.getReapeatInterval(), TimeUnit.MILLISECONDS);
				} else {
					future = scheduler.schedule(
							new StartProcessOnExpiredTimer(job.getId(), job.getProcessId(), true, -1),
							calculateDelay(job.getFireTime()), TimeUnit.MILLISECONDS);
				}
			}
			scheduledJobs.put(job.getId(), future);

		} catch (IOException e) {
			LOGGER.warn("Unable to load stored scheduled job with id {}", path.getFileName().toString(), e);
		}
	}

	protected void removeScheduledJob(String id) {
		Path path = Paths.get(storage, id);
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected long calculateDelay(ZonedDateTime expirationDate) {
		return Duration.between(ZonedDateTime.now(), expirationDate).toMillis();
	}

	protected Runnable processJobByDescription(ProcessJobDescription description) {
		return new StartProcessOnExpiredTimer(description.id(),
				description.processId() + version(description.processVersion()), true, -1);

	}

	protected Runnable repeatableProcessJobByDescription(ProcessJobDescription description) {
		return new StartProcessOnExpiredTimer(description.id(),
				description.processId() + version(description.processVersion()), false,
				description.expirationTime().repeatLimit());
	}

	protected String version(String version) {
		if (version != null && !version.trim().isEmpty()) {
			return "_" + version.replaceAll("\\.", "_");
		}
		return "";
	}

	private class SignalProcessInstanceOnExpiredTimer implements Runnable {

		private final String id;
		private final String processId;
		private boolean removeAtExecution;
		private String processInstanceId;
		private Integer limit;

		private SignalProcessInstanceOnExpiredTimer(String id, String processId, String processInstanceId,
				boolean removeAtExecution, Integer limit) {
			this.id = id;
			this.processId = processId;
			this.processInstanceId = processInstanceId;
			this.removeAtExecution = removeAtExecution;
			this.limit = limit;
		}

		@Override
		public void run() {
			try {
				LOGGER.debug("Job {} started", id);

				Process<?> process = mappedProcesses.get(processId);
				if (process == null) {
					LOGGER.warn("No process found for process id {}", processId);
					return;
				}

				UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
					Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
							.findById(processInstanceId);
					if (processInstanceFound.isPresent()) {
						ProcessInstance<?> processInstance = processInstanceFound.get();
						String[] ids = id.split("_");
						processInstance
								.send(Sig.of("timerTriggered", TimerInstance.with(Long.parseLong(ids[1]), id, limit)));
						if (limit == 0) {
							scheduledJobs.remove(id).cancel(false);
							removeScheduledJob(id);
						}
					} else {
						// since owning process instance does not exist cancel timers
						scheduledJobs.remove(id).cancel(false);
						removeScheduledJob(id);
					}

					return null;
				});
				LOGGER.debug("Job {} completed", id);
			} finally {
				if (removeAtExecution) {
					scheduledJobs.remove(id);
					removeScheduledJob(id);
				}
			}
		}
	}

	private class StartProcessOnExpiredTimer implements Runnable {

		private final String id;
		private final String processId;
		private boolean removeAtExecution;

		private Integer limit;

		private StartProcessOnExpiredTimer(String id, String processId, boolean removeAtExecution, Integer limit) {
			this.id = id;
			this.processId = processId;
			this.removeAtExecution = removeAtExecution;
			this.limit = limit;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void run() {
			try {
				LOGGER.debug("Job {} started", id);

				Process process = mappedProcesses.get(processId);
				if (process == null) {
					LOGGER.warn("No process found for process id {}", processId);
					return;
				}

				UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
					ProcessInstance<?> pi = process.createInstance(process.createModel());
					if (pi != null) {
						pi.start(TRIGGER, null, null);
					}

					return null;
				});
				limit--;
				if (limit == 0) {
					scheduledJobs.remove(id).cancel(false);
					removeScheduledJob(id);
				}
				LOGGER.debug("Job {} completed", id);
			} finally {
				if (removeAtExecution) {
					scheduledJobs.remove(id);
					removeScheduledJob(id);
				}
			}
		}
	}

}
