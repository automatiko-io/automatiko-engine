
package io.automatik.engine.services.jobs.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.auth.IdentityProvider;
import io.automatik.engine.api.auth.TrustedIdentityProvider;
import io.automatik.engine.api.jobs.JobDescription;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.services.uow.UnitOfWorkExecutor;

public class InMemoryJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryJobService.class);
    private static final String TRIGGER = "timer";

    protected final ScheduledThreadPoolExecutor scheduler;
    protected final ProcessRuntime processRuntime;
    protected final UnitOfWorkManager unitOfWorkManager;

    protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    public InMemoryJobService(ProcessRuntime processRuntime, UnitOfWorkManager unitOfWorkManager) {
        this(1, processRuntime, unitOfWorkManager);
    }

    public InMemoryJobService(int threadPoolSize, ProcessRuntime processRuntime, UnitOfWorkManager unitOfWorkManager) {
        this.scheduler = new ScheduledThreadPoolExecutor(threadPoolSize);
        this.processRuntime = processRuntime;
        this.unitOfWorkManager = unitOfWorkManager;
    }

    @Override
    public String scheduleProcessJob(ProcessJobDescription description) {
        LOGGER.debug("ScheduleProcessJob: {}", description);
        ScheduledFuture<?> future = null;
        if (description.expirationTime().repeatInterval() != null) {
            future = scheduler.scheduleAtFixedRate(repeatableProcessJobByDescription(description),
                    calculateDelay(description), description.expirationTime().repeatInterval(), TimeUnit.MILLISECONDS);
        } else {
            future = scheduler.schedule(processJobByDescription(description), calculateDelay(description),
                    TimeUnit.MILLISECONDS);
        }
        scheduledJobs.put(description.id(), future);
        return description.id();
    }

    @Override
    public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {
        ScheduledFuture<?> future = null;

        if (description.expirationTime().repeatInterval() != null) {
            future = scheduler.scheduleAtFixedRate(
                    new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                            description.processInstanceId(), false,
                            description.expirationTime().repeatLimit()),
                    calculateDelay(description), description.expirationTime().repeatInterval(), TimeUnit.MILLISECONDS);
        } else {
            future = scheduler.schedule(new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                    description.processInstanceId(), true, -1), calculateDelay(description), TimeUnit.MILLISECONDS);
        }
        scheduledJobs.put(description.id(), future);
        return description.id();
    }

    @Override
    public boolean cancelJob(String id) {
        LOGGER.debug("Cancel Job: {}", id);
        if (scheduledJobs.containsKey(id)) {
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
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() + remainingTime),
                        ZoneId.systemDefault());
            }
        }

        return null;
    }

    protected long calculateDelay(JobDescription description) {
        return Duration.between(ZonedDateTime.now(), description.expirationTime().get()).toMillis();
    }

    protected Runnable processJobByDescription(ProcessJobDescription description) {
        if (description.process() != null) {
            return new StartProcessOnExpiredTimer(description.id(), description.process(), true, -1);
        } else {
            return new LegacyStartProcessOnExpiredTimer(description.id(), description.processId(), true, -1);
        }
    }

    protected Runnable repeatableProcessJobByDescription(ProcessJobDescription description) {
        if (description.process() != null) {
            return new StartProcessOnExpiredTimer(description.id(), description.process(), false,
                    description.expirationTime().repeatLimit());
        } else {
            return new LegacyStartProcessOnExpiredTimer(description.id(), description.processId(), false,
                    description.expirationTime().repeatLimit());
        }
    }

    private class SignalProcessInstanceOnExpiredTimer implements Runnable {

        private final String id;
        private boolean removeAtExecution;
        private String processInstanceId;
        private String trigger;
        private Integer limit;

        private SignalProcessInstanceOnExpiredTimer(String id, String trigger, String processInstanceId,
                boolean removeAtExecution,
                Integer limit) {
            this.id = id;
            this.processInstanceId = processInstanceId;
            this.trigger = trigger;
            this.removeAtExecution = removeAtExecution;
            this.limit = limit;
        }

        @Override
        public void run() {
            try {
                LOGGER.debug("Job {} started", id);
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    ProcessInstance pi = processRuntime.getProcessInstance(processInstanceId);
                    if (pi != null) {
                        String[] ids = id.split("_");
                        limit--;
                        pi.signalEvent(trigger, TimerInstance.with(Long.valueOf(ids[1]), id, limit));
                        if (limit == 0) {
                            Optional.ofNullable(scheduledJobs.remove(id)).ifPresent(s -> s.cancel(false));
                        }
                    } else {
                        // since owning process instance does not exist cancel timers
                        scheduledJobs.remove(id).cancel(false);
                    }

                    return null;
                });
                LOGGER.debug("Job {} completed", id);
            } finally {
                if (removeAtExecution) {
                    scheduledJobs.remove(id);
                }
            }
        }
    }

    private class StartProcessOnExpiredTimer implements Runnable {

        private final String id;

        private boolean removeAtExecution;
        @SuppressWarnings("rawtypes")
        private io.automatik.engine.api.workflow.Process process;

        private Integer limit;

        private StartProcessOnExpiredTimer(String id, io.automatik.engine.api.workflow.Process<?> process,
                boolean removeAtExecution, Integer limit) {
            this.id = id;
            this.process = process;
            this.removeAtExecution = removeAtExecution;
            this.limit = limit;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                LOGGER.debug("Job {} started", id);
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    io.automatik.engine.api.workflow.ProcessInstance<?> pi = process
                            .createInstance(process.createModel());
                    if (pi != null) {
                        pi.start(TRIGGER, null, null);
                    }

                    return null;
                });
                limit--;
                if (limit == 0) {
                    scheduledJobs.remove(id).cancel(false);
                }
                LOGGER.debug("Job {} completed", id);
            } finally {
                if (removeAtExecution) {
                    scheduledJobs.remove(id);
                }
            }
        }
    }

    private class LegacyStartProcessOnExpiredTimer implements Runnable {

        private final String id;

        private boolean removeAtExecution;
        private String processId;

        private Integer limit;

        private LegacyStartProcessOnExpiredTimer(String id, String processId, boolean removeAtExecution,
                Integer limit) {
            this.id = id;
            this.processId = processId;
            this.removeAtExecution = removeAtExecution;
            this.limit = limit;
        }

        @Override
        public void run() {
            try {
                LOGGER.debug("Job {} started", id);
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    ProcessInstance pi = processRuntime.createProcessInstance(processId, null);
                    if (pi != null) {
                        processRuntime.startProcessInstance(pi.getId(), TRIGGER, null);
                    }

                    return null;
                });
                limit--;
                if (limit == 0) {
                    scheduledJobs.remove(id).cancel(false);
                }
                LOGGER.debug("Job {} completed", id);
            } finally {
                if (removeAtExecution) {
                    scheduledJobs.remove(id);
                }
            }
        }
    }
}
