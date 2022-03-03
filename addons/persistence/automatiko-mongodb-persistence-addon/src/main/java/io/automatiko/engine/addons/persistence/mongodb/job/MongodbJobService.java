package io.automatiko.engine.addons.persistence.mongodb.job;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.set;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.UpdateResult;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.config.MongodbJobsConfig;
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
public class MongodbJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongodbJobService.class);

    private static final String INSTANCE_ID_FIELD = "jobInstanceId";
    private static final String FIRE_AT_FIELD = "jobFireAt";
    private static final String OWNER_INSTANCE_ID_FIELD = "jobOwnerInstanceId";
    private static final String OWNER_DEF_ID_FIELD = "jobOwnerDefinitionId";
    private static final String TRIGGER_TYPE_FIELD = "jobTriggerType";
    private static final String STATUS_FIELD = "jobStatus";
    private static final String FIRE_LIMIT_FIELD = "jobFireLimit";
    private static final String REPEAT_INTERVAL_FIELD = "jobRepeatInterval";
    private static final String EXPRESSION_FIELD = "jobExpression";

    protected final MongoClient mongoClient;

    protected final UnitOfWorkManager unitOfWorkManager;

    protected final ScheduledThreadPoolExecutor scheduler;

    protected final ScheduledThreadPoolExecutor loadScheduler;

    protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();
    protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    protected final String tableName = "atk_jobs";

    private MongodbJobsConfig config;

    @Inject
    public MongodbJobService(MongoClient mongoClient,
            MongodbJobsConfig config,
            Processes processes, Application application) {
        this.mongoClient = mongoClient;
        this.config = config;
        processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

        this.unitOfWorkManager = application.unitOfWorkManager();

        this.scheduler = new ScheduledThreadPoolExecutor(config.threads().orElse(1),
                r -> new Thread(r, "automatiko-jobs-executor"));
        this.loadScheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "automatiko-jobs-loader"));
    }

    public void start(@Observes StartupEvent event) {

        collection().createIndex(Indexes.ascending(INSTANCE_ID_FIELD));
        collection().createIndex(Indexes.descending(FIRE_AT_FIELD));

        loadScheduler.scheduleAtFixedRate(() -> {
            try {
                long next = LocalDateTime.now().plus(Duration.ofMinutes(config.interval().orElse(10L)))
                        .atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli();

                FindIterable<Document> jobs = collection().find(lt(FIRE_AT_FIELD, next));

                for (Document job : jobs) {

                    if (job.getString(OWNER_INSTANCE_ID_FIELD) == null) {
                        ProcessJobDescription description = ProcessJobDescription.of(build(job.getString(EXPRESSION_FIELD)),
                                null,
                                job.getString(OWNER_DEF_ID_FIELD));

                        scheduledJobs.computeIfAbsent(job.getString(INSTANCE_ID_FIELD), k -> {
                            return log(job.getString(INSTANCE_ID_FIELD),
                                    scheduler.schedule(new StartProcessOnExpiredTimer(job.getString(INSTANCE_ID_FIELD),
                                            job.getString(OWNER_DEF_ID_FIELD), -1, description),
                                            Duration.between(LocalDateTime.now(),
                                                    ZonedDateTime.ofInstant(
                                                            Instant.ofEpochMilli(job.getLong(FIRE_AT_FIELD)),
                                                            ZoneId.systemDefault()))
                                                    .toMillis(),
                                            TimeUnit.MILLISECONDS));
                        });
                    } else {
                        ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(
                                job.getString(INSTANCE_ID_FIELD),
                                job.getString(TRIGGER_TYPE_FIELD),
                                build(job.getString(EXPRESSION_FIELD)), job.getString(OWNER_INSTANCE_ID_FIELD),
                                job.getString(OWNER_DEF_ID_FIELD), null);

                        scheduledJobs.computeIfAbsent(job.getString(INSTANCE_ID_FIELD), k -> {
                            return log(job.getString(INSTANCE_ID_FIELD), scheduler.schedule(
                                    new SignalProcessInstanceOnExpiredTimer(job.getString(INSTANCE_ID_FIELD),
                                            job.getString(TRIGGER_TYPE_FIELD),
                                            job.getString(OWNER_DEF_ID_FIELD),
                                            job.getString(OWNER_INSTANCE_ID_FIELD),
                                            job.getInteger(FIRE_LIMIT_FIELD), description),
                                    Duration.between(LocalDateTime.now(), ZonedDateTime.ofInstant(
                                            Instant.ofEpochMilli(job.getLong(FIRE_AT_FIELD)),
                                            ZoneId.systemDefault())).toMillis(),
                                    TimeUnit.MILLISECONDS));
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while loading jobs from cassandra", e);
            }
        }, 1, config.interval().orElse(10L) * 60, TimeUnit.SECONDS);
    }

    public void shutdown(@Observes ShutdownEvent event) {
        this.loadScheduler.shutdownNow();

        this.scheduler.shutdown();
    }

    @Override
    public String scheduleProcessJob(ProcessJobDescription description) {
        LOGGER.debug("ScheduleProcessJob: {}", description);
        Document job = new Document();
        if (description.expirationTime().repeatInterval() != null) {
            job.append(INSTANCE_ID_FIELD, description.id())
                    .append(OWNER_DEF_ID_FIELD, description.processId() + version(description.processVersion()))
                    .append(STATUS_FIELD, "scheduled")
                    .append(FIRE_AT_FIELD,
                            description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli())
                    .append(FIRE_LIMIT_FIELD, description.expirationTime().repeatLimit())
                    .append(REPEAT_INTERVAL_FIELD, description.expirationTime().repeatInterval())
                    .append(EXPRESSION_FIELD, description.expirationTime().expression());
        } else {

            job.append(INSTANCE_ID_FIELD, description.id())
                    .append(OWNER_DEF_ID_FIELD, description.processId() + version(description.processVersion()))
                    .append(STATUS_FIELD, "scheduled")
                    .append(FIRE_AT_FIELD,
                            description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli())
                    .append(FIRE_LIMIT_FIELD, description.expirationTime().repeatLimit())
                    .append(EXPRESSION_FIELD, description.expirationTime().expression());

        }
        collection().insertOne(job);
        if (description.expirationTime().get().toLocalDateTime()
                .isBefore(LocalDateTime.now().plusMinutes(config.interval().orElse(10L)))) {

            scheduledJobs.computeIfAbsent(description.id(), k -> {
                return scheduler.schedule(processJobByDescription(description),
                        calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);
            });
        }

        return description.id();
    }

    @Override
    public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {

        Document job = new Document();
        if (description.expirationTime().repeatInterval() != null) {

            job.append(INSTANCE_ID_FIELD, description.id())
                    .append(TRIGGER_TYPE_FIELD, description.triggerType())
                    .append(OWNER_DEF_ID_FIELD, description.processId() + version(description.processVersion()))
                    .append(OWNER_INSTANCE_ID_FIELD, description.processInstanceId())
                    .append(STATUS_FIELD, "scheduled")
                    .append(FIRE_AT_FIELD,
                            description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli())
                    .append(FIRE_LIMIT_FIELD, description.expirationTime().repeatLimit())
                    .append(REPEAT_INTERVAL_FIELD, description.expirationTime().repeatInterval())
                    .append(EXPRESSION_FIELD, description.expirationTime().expression());

        } else {

            job.append(INSTANCE_ID_FIELD, description.id())
                    .append(TRIGGER_TYPE_FIELD, description.triggerType())
                    .append(OWNER_DEF_ID_FIELD, description.processId() + version(description.processVersion()))
                    .append(OWNER_INSTANCE_ID_FIELD, description.processInstanceId())
                    .append(STATUS_FIELD, "scheduled")
                    .append(FIRE_AT_FIELD,
                            description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli())
                    .append(FIRE_LIMIT_FIELD, description.expirationTime().repeatLimit())
                    .append(EXPRESSION_FIELD, description.expirationTime().expression());
        }

        collection().insertOne(job);

        if (description.expirationTime().get().toLocalDateTime()
                .isBefore(LocalDateTime.now().plusMinutes(config.interval().orElse(10L)))) {

            scheduledJobs.computeIfAbsent(description.id(), k -> {
                return log(description.id(), scheduler.schedule(
                        new SignalProcessInstanceOnExpiredTimer(description.id(), description.triggerType(),
                                description.processId() + version(description.processVersion()),
                                description.processInstanceId(), description.expirationTime().repeatLimit(), description),
                        calculateDelay(description.expirationTime().get()),
                        TimeUnit.MILLISECONDS));
            });
        }

        return description.id();
    }

    @Override
    public boolean cancelJob(String id) {

        removeScheduledJob(id);

        return true;
    }

    @Override
    public ZonedDateTime getScheduledTime(String id) {

        Document found = collection().find(and(eq(INSTANCE_ID_FIELD, id)))
                .projection(Projections
                        .fields(Projections.include(INSTANCE_ID_FIELD, FIRE_AT_FIELD)))
                .first();

        if (found != null) {
            Long fireAt = found.getLong(FIRE_AT_FIELD);

            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(fireAt), ZoneId.systemDefault());

        } else {
            return null;
        }

    }

    protected long calculateDelay(ZonedDateTime expirationDate) {
        return Duration.between(ZonedDateTime.now(), expirationDate).toMillis();
    }

    protected Runnable processJobByDescription(ProcessJobDescription description) {
        return new StartProcessOnExpiredTimer(description.id(),
                description.process().id(), description.expirationTime().repeatLimit(), description);

    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    protected void removeScheduledJob(String id) {

        collection().findOneAndDelete(eq(INSTANCE_ID_FIELD, id));
    }

    protected void updateRepeatableJob(String id) {

        Document job = collection().find(eq(INSTANCE_ID_FIELD, id))
                .projection(Projections
                        .fields(Projections.include(INSTANCE_ID_FIELD, FIRE_AT_FIELD)))
                .first();
        if (job != null) {

            Integer limit = job.getInteger(FIRE_LIMIT_FIELD) - 1;
            Long repeat = job.getLong(REPEAT_INTERVAL_FIELD);
            ZonedDateTime fireTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(job.getLong(FIRE_AT_FIELD)),
                    ZoneId.systemDefault());

            job.put(STATUS_FIELD, "scheduled");
            job.put(FIRE_LIMIT_FIELD, limit);
            job.put(FIRE_AT_FIELD, fireTime.plus(repeat, ChronoUnit.MILLIS).toInstant().toEpochMilli());

            collection().updateOne(eq(INSTANCE_ID_FIELD, id), job);

            if (job.getString(OWNER_INSTANCE_ID_FIELD) == null) {
                ProcessJobDescription description = ProcessJobDescription.of(build(job.getString(EXPRESSION_FIELD)), null,
                        job.getString(OWNER_DEF_ID_FIELD));

                scheduledJobs.computeIfAbsent(job.getString(INSTANCE_ID_FIELD), k -> {
                    return log(job.getString(INSTANCE_ID_FIELD),
                            scheduler.schedule(new StartProcessOnExpiredTimer(job.getString(INSTANCE_ID_FIELD),
                                    job.getString(OWNER_DEF_ID_FIELD), limit, description),
                                    Duration.between(LocalDateTime.now(), fireTime).toMillis(),
                                    TimeUnit.MILLISECONDS));
                });
            } else {
                ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(job.getString(INSTANCE_ID_FIELD),
                        job.getString(TRIGGER_TYPE_FIELD),
                        build(job.getString(EXPRESSION_FIELD)), job.getString(OWNER_INSTANCE_ID_FIELD),
                        job.getString(OWNER_DEF_ID_FIELD), null);

                scheduledJobs.computeIfAbsent(job.getString(INSTANCE_ID_FIELD), k -> {
                    return log(job.getString(INSTANCE_ID_FIELD), scheduler.scheduleAtFixedRate(
                            new SignalProcessInstanceOnExpiredTimer(job.getString(INSTANCE_ID_FIELD),
                                    job.getString(TRIGGER_TYPE_FIELD),
                                    job.getString(OWNER_DEF_ID_FIELD),
                                    job.getString(OWNER_INSTANCE_ID_FIELD), limit, description),
                            Duration.between(LocalDateTime.now(), fireTime).toMillis(), repeat,
                            TimeUnit.MILLISECONDS));
                });
            }
        }
    }

    protected ScheduledFuture<?> log(String jobId, ScheduledFuture<?> future) {
        LOGGER.info("Next fire of job {} is in {} seconds ", jobId, future.getDelay(TimeUnit.SECONDS));

        return future;
    }

    protected ExpirationTime build(String expression) {
        if (expression != null) {
            return CronExpirationTime.of(expression);
        }

        return new NoOpExpirationTime();
    }

    protected MongoCollection<Document> collection() {
        MongoDatabase database = mongoClient.getDatabase(config.database().orElse("automatiko"));
        return database.getCollection(tableName);
    }

    private class SignalProcessInstanceOnExpiredTimer implements Runnable {

        private final String id;
        private final String processId;
        private String processInstanceId;

        private final String trigger;
        private Integer limit;

        private ProcessInstanceJobDescription description;

        private SignalProcessInstanceOnExpiredTimer(String id, String trigger, String processId, String processInstanceId,
                Integer limit, ProcessInstanceJobDescription description) {
            this.id = id;
            this.processId = processId;
            this.processInstanceId = processInstanceId;
            this.trigger = trigger;
            this.limit = limit;

            this.description = description;
        }

        @Override
        public void run() {
            LOGGER.debug("Job {} started", id);

            try {
                UpdateResult result = collection().updateOne(and(eq(INSTANCE_ID_FIELD, id), eq(STATUS_FIELD, "scheduled")),
                        set(STATUS_FIELD, "taken"));

                if (result.getModifiedCount() == 0) {
                    scheduledJobs.remove(id).cancel(true);
                    return;
                }

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
                        scheduledJobs.remove(id).cancel(false);
                        if (description.expirationTime().next() != null) {
                            removeScheduledJob(id);
                            scheduleProcessInstanceJob(description);
                        } else if (limit > 0) {
                            updateRepeatableJob(id);
                        } else {
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
            } catch (MongoWriteConcernException | MongoWriteException rnf) {
                scheduledJobs.remove(id).cancel(true);
            }

        }
    }

    private class StartProcessOnExpiredTimer implements Runnable {

        private final String id;
        private final String processId;

        private Integer limit;

        private ProcessJobDescription description;

        private StartProcessOnExpiredTimer(String id, String processId, Integer limit, ProcessJobDescription description) {
            this.id = id;
            this.processId = processId;
            this.limit = limit;

            this.description = description;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void run() {
            LOGGER.debug("Job {} started", id);

            try {
                UpdateResult result = collection().updateOne(and(eq(INSTANCE_ID_FIELD, id), eq(STATUS_FIELD, "scheduled")),
                        set(STATUS_FIELD, "taken"));

                if (result.getModifiedCount() == 0) {
                    scheduledJobs.remove(id).cancel(true);
                    return;
                }

                Process process = mappedProcesses.get(processId);
                if (process == null) {
                    LOGGER.warn("No process found for process id {}", processId);
                    return;
                }
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    ProcessInstance<?> pi = process.createInstance(process.createModel());
                    if (pi != null) {
                        pi.start("timer", null, null);
                    }
                    scheduledJobs.remove(id).cancel(false);
                    limit--;
                    if (description.expirationTime().next() != null) {
                        removeScheduledJob(id);
                        scheduleProcessJob(description);
                    } else if (limit > 0) {
                        updateRepeatableJob(id);
                    } else {
                        removeScheduledJob(id);
                    }
                    return null;
                });

                LOGGER.debug("Job {} completed", id);
            } catch (MongoWriteConcernException | MongoWriteException rnf) {
                scheduledJobs.remove(id).cancel(true);
            }
        }
    }

}
