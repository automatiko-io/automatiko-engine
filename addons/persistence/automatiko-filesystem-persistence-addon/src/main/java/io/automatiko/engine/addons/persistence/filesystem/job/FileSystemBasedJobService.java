package io.automatiko.engine.addons.persistence.filesystem.job;

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

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.Processes;
import io.automatiko.engine.services.time.TimerInstance;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.base.core.timer.CronExpirationTime;
import io.automatiko.engine.workflow.base.core.timer.NoOpExpirationTime;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

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
    public FileSystemBasedJobService(
            @ConfigProperty(name = "quarkus.automatiko.jobs.filesystem.path", defaultValue = ".") String storage,
            @ConfigProperty(name = "quarkus.automatiko.jobs.filesystem.threads", defaultValue = "1") int threads,
            Processes processes, Application application) {
        this.storage = storage;
        processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

        this.unitOfWorkManager = application.unitOfWorkManager();

        this.scheduler = new ScheduledThreadPoolExecutor(threads, r -> new Thread(r, "automatiko-jobs-executor"));
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
                    description.expirationTime().get(), description.expirationTime().expression());
        } else {
            future = scheduler.schedule(processJobByDescription(description),
                    calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);

            scheduledJob = new ScheduledJob(description.id(), description.processId(), true, -1, null,
                    description.expirationTime().get(), description.expirationTime().expression());
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
                    new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                            description.processId() + version(description.processVersion()),
                            description.processInstanceId(), false, description.expirationTime().repeatLimit(), description),
                    calculateDelay(description.expirationTime().get()), description.expirationTime().repeatInterval(),
                    TimeUnit.MILLISECONDS);

            scheduledJob = new ScheduledJob(description.id(), description.triggerType(),
                    description.processId() + version(description.processVersion()), false,
                    description.processInstanceId(), description.expirationTime().repeatLimit(),
                    description.expirationTime().repeatInterval(), description.expirationTime().get(),
                    description.expirationTime().expression());
        } else {
            future = scheduler.schedule(
                    new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                            description.processId() + version(description.processVersion()),
                            description.processInstanceId(), true, description.expirationTime().repeatLimit(), description),
                    calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);

            scheduledJob = new ScheduledJob(description.id(), description.triggerType(),
                    description.processId() + version(description.processVersion()), true,
                    description.processInstanceId(), description.expirationTime().repeatLimit(), null,
                    description.expirationTime().get(), description.expirationTime().expression());
        }
        scheduledJobs.put(description.id(), future);
        storeScheduledJob(scheduledJob);
        return description.id();
    }

    @Override
    public boolean cancelJob(String id) {
        LOGGER.debug("Cancel Job: {}", id);
        if (id != null && scheduledJobs.containsKey(id)) {
            removeScheduledJob(id);
            return scheduledJobs.remove(id).cancel(false);
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
                ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(job.getId(), job.getTriggerType(),
                        build(job), job.getProcessInstanceId(), job.getProcessId(), null);
                if (job.getReapeatInterval() != null) {
                    future = scheduler.scheduleAtFixedRate(
                            new SignalProcessInstanceOnExpiredTimer(job.getId(), job.getTriggerType(), job.getProcessId(),
                                    job.getProcessInstanceId(), false, job.getLimit(), description),
                            calculateDelay(job.getFireTime()), job.getReapeatInterval(), TimeUnit.MILLISECONDS);

                } else {
                    future = scheduler.schedule(
                            new SignalProcessInstanceOnExpiredTimer(job.getId(), job.getTriggerType(), job.getProcessId(),
                                    job.getProcessInstanceId(), true, description.expirationTime().repeatLimit(), description),
                            calculateDelay(job.getFireTime()), TimeUnit.MILLISECONDS);
                }

            } else {
                ProcessJobDescription description = ProcessJobDescription.of(build(job), null, job.getProcessId());

                if (job.getReapeatInterval() != null) {
                    future = scheduler.scheduleAtFixedRate(
                            new StartProcessOnExpiredTimer(job.getId(), job.getProcessId(), false, job.getLimit(), description),
                            calculateDelay(job.getFireTime()), job.getReapeatInterval(), TimeUnit.MILLISECONDS);
                } else {
                    future = scheduler.schedule(
                            new StartProcessOnExpiredTimer(job.getId(), job.getProcessId(), true, -1, description),
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
        long delay = Duration.between(ZonedDateTime.now(), expirationDate).toMillis();
        if (delay < 0) {
            delay = 1000;
        }
        return log(expirationDate, delay);
    }

    protected Runnable processJobByDescription(ProcessJobDescription description) {
        return new StartProcessOnExpiredTimer(description.id(),
                description.processId() + version(description.processVersion()), true, -1, description);

    }

    protected Runnable repeatableProcessJobByDescription(ProcessJobDescription description) {
        return new StartProcessOnExpiredTimer(description.id(),
                description.processId() + version(description.processVersion()), false,
                description.expirationTime().repeatLimit(), description);
    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    protected ExpirationTime build(ScheduledJob job) {
        if (job.getExpression() != null) {
            return CronExpirationTime.of(job.getExpression());
        }

        return new NoOpExpirationTime();
    }

    protected long log(ZonedDateTime dt, long delay) {
        LOGGER.debug("Timer scheduled for date {} will expire in {}", dt, delay);
        return delay;
    }

    private class SignalProcessInstanceOnExpiredTimer implements Runnable {

        private final String id;
        private final String processId;
        private boolean removeAtExecution;
        private String processInstanceId;
        private final String trigger;
        private Integer limit;

        private ProcessInstanceJobDescription description;

        private SignalProcessInstanceOnExpiredTimer(String id, String trigger, String processId, String processInstanceId,
                boolean removeAtExecution, Integer limit, ProcessInstanceJobDescription description) {
            this.id = id;
            this.trigger = trigger;
            this.processId = processId;
            this.processInstanceId = processInstanceId;
            this.removeAtExecution = removeAtExecution;
            this.limit = limit;

            this.description = description;
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
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                            .findById(processInstanceId);
                    if (processInstanceFound.isPresent()) {
                        ProcessInstance<?> processInstance = processInstanceFound.get();
                        String[] ids = id.split("_");
                        processInstance
                                .send(Sig.of(trigger, TimerInstance.with(Long.parseLong(ids[1]), id, limit)));
                        if (limit == 0) {
                            Optional.ofNullable(scheduledJobs.remove(id)).ifPresent(j -> j.cancel(false));
                            removeScheduledJob(id);
                        }
                    } else {
                        // since owning process instance does not exist cancel timers
                        Optional.ofNullable(scheduledJobs.remove(id)).ifPresent(j -> j.cancel(false));
                        removeScheduledJob(id);
                    }

                    return null;
                });
                LOGGER.debug("Job {} completed", id);
            } finally {
                if (description.expirationTime().next() != null) {
                    scheduleProcessInstanceJob(description);
                } else if (removeAtExecution) {
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

        private ProcessJobDescription description;

        private StartProcessOnExpiredTimer(String id, String processId, boolean removeAtExecution, Integer limit,
                ProcessJobDescription description) {
            this.id = id;
            this.processId = processId;
            this.removeAtExecution = removeAtExecution;
            this.limit = limit;

            this.description = description;
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
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    ProcessInstance<?> pi = process.createInstance(process.createModel());
                    if (pi != null) {
                        pi.start(TRIGGER, null, null);
                    }

                    return null;
                });
                limit--;
                if (limit == 0) {
                    Optional.ofNullable(scheduledJobs.remove(id)).ifPresent(j -> j.cancel(false));
                    removeScheduledJob(id);
                }
                LOGGER.debug("Job {} completed", id);
            } finally {
                if (description.expirationTime().next() != null) {
                    scheduleProcessJob(description);
                } else if (removeAtExecution) {
                    scheduledJobs.remove(id);
                    removeScheduledJob(id);
                }
            }
        }
    }

}
