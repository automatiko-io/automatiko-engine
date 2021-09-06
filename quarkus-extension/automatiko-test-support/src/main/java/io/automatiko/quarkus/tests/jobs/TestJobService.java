package io.automatiko.quarkus.tests.jobs;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.jobs.JobDescription;
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

@Alternative
@ApplicationScoped
public class TestJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobService.class);

    private static final String TRIGGER = "timer";

    private Map<String, JobDescription> jobs = new ConcurrentHashMap<>();

    protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();

    protected final UnitOfWorkManager unitOfWorkManager;

    public TestJobService(Processes processes, Application application) {
        processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

        this.unitOfWorkManager = application.unitOfWorkManager();
    }

    @Override
    public String scheduleProcessJob(ProcessJobDescription description) {
        LOGGER.debug("scheduling process job {}", description);
        jobs.put(description.id(), description);
        return description.id();
    }

    @Override
    public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {
        LOGGER.debug("scheduling process instance job {}", description);
        jobs.put(description.id(), description);
        return description.id();
    }

    @Override
    public boolean cancelJob(String id) {
        LOGGER.debug("Removing job {}", id);
        JobDescription removed = jobs.remove(id);
        return removed != null;
    }

    @Override
    public ZonedDateTime getScheduledTime(String id) {
        JobDescription job = jobs.remove(id);

        if (job != null) {
            return job.expirationTime().get();
        }
        return null;
    }

    public Set<String> jobIds() {
        return jobs.keySet();
    }

    public List<ProcessJobDescription> processJobs() {
        return jobs.values().stream().filter(job -> job instanceof ProcessJobDescription).map(ProcessJobDescription.class::cast)
                .collect(Collectors.toList());
    }

    public List<ProcessJobDescription> processJobs(String processId) {
        return jobs.values().stream().filter(job -> job instanceof ProcessJobDescription).map(ProcessJobDescription.class::cast)
                .filter(pjob -> pjob.processId().equals(processId)).collect(Collectors.toList());
    }

    public List<ProcessInstanceJobDescription> processInstanceJobs() {
        return jobs.values().stream().filter(job -> job instanceof ProcessInstanceJobDescription)
                .map(ProcessInstanceJobDescription.class::cast)
                .collect(Collectors.toList());
    }

    public List<ProcessInstanceJobDescription> processInstanceJobs(String processId) {
        return jobs.values().stream().filter(job -> job instanceof ProcessInstanceJobDescription)
                .map(ProcessInstanceJobDescription.class::cast)
                .filter(pjob -> pjob.processId().equals(processId)).collect(Collectors.toList());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void triggerProcessJob(String jobId) {

        ProcessJobDescription job = (ProcessJobDescription) jobs.remove(jobId);

        if (job == null) {
            throw new IllegalArgumentException("Job with id " + jobId + " not found");
        }
        int limit = job.expirationTime().repeatLimit();
        try {
            LOGGER.debug("Job {} started", job.id());

            Process process = mappedProcesses.get(job.processId());
            if (process == null) {
                LOGGER.warn("No process found for process id {}", job.processId());
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
                jobs.remove(jobId);
            }
            LOGGER.debug("Job {} completed", job.id());
        } finally {
            if (job.expirationTime().next() != null) {
                jobs.remove(jobId);
                scheduleProcessJob(job);
            } else {
                jobs.remove(jobId);
            }
        }
    }

    public void triggerProcessInstanceJob(String jobId) {

        LOGGER.debug("Job {} started", jobId);

        ProcessInstanceJobDescription job = (ProcessInstanceJobDescription) jobs.remove(jobId);

        if (job == null) {
            throw new IllegalArgumentException("Job with id " + jobId + " not found");
        }
        try {
            Process<?> process = mappedProcesses.get(job.processId());
            if (process == null) {
                LOGGER.warn("No process found for process id {}", job.processId());
                return;
            }
            IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
            UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                        .findById(job.processInstanceId());
                if (processInstanceFound.isPresent()) {
                    ProcessInstance<?> processInstance = processInstanceFound.get();
                    String[] ids = job.id().split("_");
                    processInstance
                            .send(Sig.of(job.triggerType(),
                                    TimerInstance.with(Long.parseLong(ids[1]), job.id(), job.expirationTime().repeatLimit())));
                    if (job.expirationTime().repeatLimit() == 0) {

                        jobs.remove(jobId);
                    }
                } else {
                    // since owning process instance does not exist cancel timers
                    jobs.remove(jobId);
                }

                return null;
            });
            LOGGER.debug("Job {} completed", job.id());
        } finally {
            if (job.expirationTime().next() != null) {
                jobs.remove(jobId);
                scheduleProcessInstanceJob(job);
            } else {
                jobs.remove(jobId);
            }
        }
    }

}
