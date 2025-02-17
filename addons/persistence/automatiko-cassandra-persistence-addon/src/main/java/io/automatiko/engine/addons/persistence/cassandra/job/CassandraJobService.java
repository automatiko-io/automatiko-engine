package io.automatiko.engine.addons.persistence.cassandra.job;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createKeyspace;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.servererrors.QueryExecutionException;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.insert.Insert;
import com.datastax.oss.driver.api.querybuilder.schema.CreateIndex;
import com.datastax.oss.driver.api.querybuilder.schema.CreateKeyspace;
import com.datastax.oss.driver.api.querybuilder.schema.CreateTable;
import com.datastax.oss.driver.api.querybuilder.select.Select;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.config.CassandraJobsConfig;
import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.Processes;
import io.automatiko.engine.services.time.TimerInstance;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.workflow.Sig;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;
import io.automatiko.engine.workflow.base.core.timer.CronExpirationTime;
import io.automatiko.engine.workflow.base.core.timer.NoOpExpirationTime;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

@ApplicationScoped
public class CassandraJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraJobService.class);

    private static final String INSTANCE_ID_FIELD = "JobInstanceId";
    private static final String FIRE_AT_FIELD = "JobFireAt";
    private static final String OWNER_INSTANCE_ID_FIELD = "JobOwnerInstanceId";
    private static final String OWNER_DEF_ID_FIELD = "JobOwnerDefinitionId";
    private static final String TRIGGER_TYPE_FIELD = "JobTriggerType";
    private static final String STATUS_FIELD = "JobStatus";
    private static final String FIRE_LIMIT_FIELD = "JobFireLimit";
    private static final String REPEAT_INTERVAL_FIELD = "JobRepeatInterval";
    private static final String EXPRESSION_FIELD = "JobExpression";

    protected final CqlSession cqlSession;

    protected final UnitOfWorkManager unitOfWorkManager;

    protected final Auditor auditor;

    protected final ScheduledThreadPoolExecutor scheduler;

    protected final ScheduledThreadPoolExecutor loadScheduler;

    protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();
    protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    protected final String tableName = "ATK_JOBS";

    private Optional<Boolean> createKeyspace;

    private Optional<Boolean> createTables;

    private Optional<String> keyspace;

    private Optional<Long> interval;

    private Optional<Integer> threads;

    @Inject
    public CassandraJobService(CqlSession cqlSession, Processes processes, Application application, Auditor auditor,
            @ConfigProperty(name = "quarkus.automatiko.persistence.disabled") Optional<Boolean> persistenceDisabled,
            @ConfigProperty(name = CassandraJobsConfig.CREATE_KEYSPACE_KEY) Optional<Boolean> createKeyspace,
            @ConfigProperty(name = CassandraJobsConfig.CREATE_TABLES_KEY) Optional<Boolean> createTables,
            @ConfigProperty(name = CassandraJobsConfig.KEYSPACE_KEY) Optional<String> keyspace,
            @ConfigProperty(name = CassandraJobsConfig.INTERVAL_KEY) Optional<Long> interval,
            @ConfigProperty(name = CassandraJobsConfig.THREADS_KEY) Optional<Integer> threads) {

        this.createKeyspace = createKeyspace;
        this.createTables = createTables;
        this.keyspace = keyspace;
        this.interval = interval;
        this.threads = threads;
        if (!persistenceDisabled.orElse(false)) {
            this.cqlSession = cqlSession;

            processes.processIds().forEach(id -> mappedProcesses.put(id, processes.processById(id)));

            if (this.createTables.orElse(Boolean.TRUE)) {
                createTable();
            }

            this.unitOfWorkManager = application.unitOfWorkManager();
            this.auditor = auditor;

            this.scheduler = new ScheduledThreadPoolExecutor(this.threads.orElse(1),
                    r -> new Thread(r, "automatiko-jobs-executor"));
            this.loadScheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "automatiko-jobs-loader"));
        } else {
            this.cqlSession = null;
            this.unitOfWorkManager = null;
            this.auditor = null;
            this.scheduler = null;
            this.loadScheduler = null;
        }
    }

    public void start(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER) StartupEvent event) {
        if (this.cqlSession != null) {
            loadScheduler.scheduleAtFixedRate(() -> {
                try {
                    long next = LocalDateTime.now().plus(Duration.ofMinutes(interval.orElse(10L)))
                            .atZone(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli();
                    Select select = selectFrom(keyspace.orElse("automatiko"), tableName).all()
                            .whereColumn(FIRE_AT_FIELD).isLessThan(literal(next)).allowFiltering();

                    ResultSet rs = cqlSession.execute(select.build());
                    List<Row> jobs = rs.all();
                    LOGGER.debug("Loaded jobs ({}) to be executed before {}", jobs.size(), next);
                    for (Row job : jobs) {

                        if (job.getString(OWNER_INSTANCE_ID_FIELD) == null) {
                            ProcessJobDescription description = ProcessJobDescription.of(build(job.getString(EXPRESSION_FIELD)),
                                    job.getString(OWNER_DEF_ID_FIELD),
                                    null);

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
                                                job.getInt(FIRE_LIMIT_FIELD), description),
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
            }, 1, interval.orElse(10L) * 60, TimeUnit.SECONDS);
        }
    }

    public void shutdown(@Observes ShutdownEvent event) {
        if (loadScheduler != null) {
            this.loadScheduler.shutdownNow();
        }
        if (scheduler != null) {
            this.scheduler.shutdown();
        }
    }

    @Override
    public String scheduleProcessJob(ProcessJobDescription description) {
        LOGGER.debug("ScheduleProcessJob: {}", description);
        Insert insert;
        if (description.expirationTime().repeatInterval() != null) {
            insert = insertInto(keyspace.orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(description.id()))
                    .value(OWNER_DEF_ID_FIELD, literal(description.processId()))
                    .value(STATUS_FIELD, literal("scheduled"))
                    .value(FIRE_AT_FIELD,
                            literal(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                    .value(FIRE_LIMIT_FIELD, literal(description.expirationTime().repeatLimit()))
                    .value(REPEAT_INTERVAL_FIELD, literal(description.expirationTime().repeatInterval()))
                    .value(EXPRESSION_FIELD, literal(description.expirationTime().expression()));
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled repeatable timer job that creates new workflow instances");

            auditor.publish(entry);
        } else {
            insert = insertInto(keyspace.orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(description.id()))
                    .value(OWNER_DEF_ID_FIELD, literal(description.processId()))
                    .value(STATUS_FIELD, literal("scheduled"))
                    .value(FIRE_AT_FIELD,
                            literal(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                    .value(FIRE_LIMIT_FIELD, literal(description.expirationTime().repeatLimit()))
                    .value(EXPRESSION_FIELD, literal(description.expirationTime().expression()));
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled one time timer job that creates new workflow instances");

            auditor.publish(entry);
        }
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(description.id()));

        ResultSet rs = cqlSession.execute(select.build());
        Row row = rs.one();

        if (row == null) {
            cqlSession.execute(description.id());
        }
        if (description.expirationTime().get().toLocalDateTime()
                .isBefore(LocalDateTime.now().plusMinutes(interval.orElse(10L)))) {

            scheduledJobs.computeIfAbsent(description.id(), k -> {
                return scheduler.schedule(processJobByDescription(description),
                        calculateDelay(description.expirationTime().get()), TimeUnit.MILLISECONDS);
            });
        }

        return description.id();
    }

    @Override
    public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {

        Insert insert;
        if (description.expirationTime().repeatInterval() != null) {

            insert = insertInto(keyspace.orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(description.id()))
                    .value(TRIGGER_TYPE_FIELD, literal(description.triggerType()))
                    .value(OWNER_DEF_ID_FIELD, literal(description.processId() + version(description.processVersion())))
                    .value(OWNER_INSTANCE_ID_FIELD, literal(description.processInstanceId()))
                    .value(STATUS_FIELD, literal("scheduled"))
                    .value(FIRE_AT_FIELD,
                            literal(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                    .value(FIRE_LIMIT_FIELD, literal(description.expirationTime().repeatLimit()))
                    .value(REPEAT_INTERVAL_FIELD, literal(description.expirationTime().repeatInterval()))
                    .value(EXPRESSION_FIELD, literal(description.expirationTime().expression()));
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled repeatable timer job for existing workflow instance");

            auditor.publish(entry);
        } else {
            insert = insertInto(keyspace.orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(description.id()))
                    .value(TRIGGER_TYPE_FIELD, literal(description.triggerType()))
                    .value(OWNER_DEF_ID_FIELD, literal(description.processId() + version(description.processVersion())))
                    .value(OWNER_INSTANCE_ID_FIELD, literal(description.processInstanceId()))
                    .value(STATUS_FIELD, literal("scheduled"))
                    .value(FIRE_AT_FIELD,
                            literal(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                    .value(FIRE_LIMIT_FIELD, literal(description.expirationTime().repeatLimit()))
                    .value(EXPRESSION_FIELD, literal(description.expirationTime().expression()));
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled one time timer job for existing workflow instance");

            auditor.publish(entry);
        }

        cqlSession.execute(insert.build());

        if (description.expirationTime().get().toLocalDateTime()
                .isBefore(LocalDateTime.now().plusMinutes(interval.orElse(10L)))) {

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
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(FIRE_AT_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id));

        ResultSet rs = cqlSession.execute(select.build());
        Row row = rs.one();
        if (row != null) {
            Long fireAt = row.getLong(FIRE_AT_FIELD);

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
        Supplier<AuditEntry> entry = () -> {
            Select select = selectFrom(keyspace.orElse("automatiko"), tableName)
                    .columns(EXPRESSION_FIELD, REPEAT_INTERVAL_FIELD, FIRE_LIMIT_FIELD, OWNER_DEF_ID_FIELD,
                            OWNER_INSTANCE_ID_FIELD, TRIGGER_TYPE_FIELD)
                    .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id));

            ResultSet rs = cqlSession.execute(select.build());
            Row row = rs.one();
            if (row != null) {
                return BaseAuditEntry.timer()
                        .add("message", "Cancelled job for existing workflow instance")
                        .add("jobId", id)
                        .add("timerExpression", row.getString(EXPRESSION_FIELD))
                        .add("timerInterval", row.getLong(REPEAT_INTERVAL_FIELD))
                        .add("timerRepeatLimit", row.getInt(FIRE_LIMIT_FIELD))
                        .add("workflowDefinitionId", row.getString(OWNER_DEF_ID_FIELD))
                        .add("workflowInstanceId", row.getString(OWNER_INSTANCE_ID_FIELD))
                        .add("triggerType", row.getString(TRIGGER_TYPE_FIELD));

            } else {
                return BaseAuditEntry.timer()
                        .add("message", "Cancelled job for existing workflow instance")
                        .add("jobId", id);
            }
        };

        auditor.publish(entry);
        Delete deleteStatement = deleteFrom(keyspace.orElse("automatiko"), tableName).whereColumn(INSTANCE_ID_FIELD)
                .isEqualTo(literal(id)).ifExists();

        cqlSession.execute(deleteStatement.build());
    }

    protected void updateRepeatableJob(String id) {
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(FIRE_AT_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id));

        ResultSet rs = cqlSession.execute(select.build());
        Row job = rs.one();
        if (job != null) {

            Integer limit = job.getInt(FIRE_LIMIT_FIELD) - 1;
            Long repeat = job.getLong(REPEAT_INTERVAL_FIELD);
            ZonedDateTime fireTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(job.getLong(FIRE_AT_FIELD)),
                    ZoneId.systemDefault());

            SimpleStatement statement = QueryBuilder.update(keyspace.orElse("automatiko"), tableName)
                    .setColumn(STATUS_FIELD, literal("scheduled"))
                    .setColumn(FIRE_LIMIT_FIELD, literal(limit))
                    .setColumn(FIRE_AT_FIELD, literal(fireTime.plus(repeat, ChronoUnit.MILLIS).toInstant().toEpochMilli()))
                    .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id))
                    .build();

            cqlSession.execute(statement);

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
        LOGGER.debug("Next fire of job {} is in {} seconds ", jobId, future.getDelay(TimeUnit.SECONDS));

        return future;
    }

    protected ExpirationTime build(String expression) {
        if (expression != null) {
            return CronExpirationTime.of(expression);
        }

        return new NoOpExpirationTime();
    }

    protected void createTable() {
        if (createKeyspace.orElse(true)) {
            CreateKeyspace createKs = createKeyspace(keyspace.orElse("automatiko")).ifNotExists()
                    .withSimpleStrategy(1);
            cqlSession.execute(createKs.build());
        }

        CreateTable createTable = SchemaBuilder.createTable(keyspace.orElse("automatiko"), tableName)
                .ifNotExists()
                .withPartitionKey(INSTANCE_ID_FIELD, DataTypes.TEXT)
                .withColumn(FIRE_AT_FIELD, DataTypes.BIGINT)
                .withColumn(OWNER_INSTANCE_ID_FIELD, DataTypes.TEXT)
                .withColumn(OWNER_DEF_ID_FIELD, DataTypes.TEXT)
                .withColumn(TRIGGER_TYPE_FIELD, DataTypes.TEXT)
                .withColumn(STATUS_FIELD, DataTypes.TEXT)
                .withColumn(FIRE_LIMIT_FIELD, DataTypes.INT)
                .withColumn(REPEAT_INTERVAL_FIELD, DataTypes.BIGINT)
                .withColumn(EXPRESSION_FIELD, DataTypes.TEXT);

        cqlSession.execute(createTable.build());

        CreateIndex index = SchemaBuilder.createIndex(tableName + "_IDX").ifNotExists()
                .onTable(keyspace.orElse("automatiko"), tableName).andColumn(FIRE_AT_FIELD);
        cqlSession.execute(index.build());
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

            SimpleStatement statement = QueryBuilder.update(keyspace.orElse("automatiko"), tableName)
                    .setColumn(STATUS_FIELD, literal("taken"))
                    .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id))
                    .ifColumn(STATUS_FIELD).isEqualTo(literal("scheduled"))
                    .build();

            try {
                boolean applied = cqlSession.execute(statement).wasApplied();

                if (!applied) {
                    scheduledJobs.remove(id).cancel(true);
                    return;
                }

                Process<?> process = mappedProcesses.get(processId);
                if (process == null) {
                    LOGGER.warn("No process found for process id {}", processId);
                    return;
                }

                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                        .add("message", "Executing timer job for existing workflow instance");

                auditor.publish(entry);
                UnitOfWorkExecutor.executeInUnitOfWork(unitOfWorkManager, () -> {
                    Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                            .findById(processInstanceId, ProcessInstanceReadMode.MUTABLE_WITH_LOCK);
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
            } catch (QueryExecutionException rnf) {
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
            SimpleStatement statement = QueryBuilder.update(keyspace.orElse("automatiko"), tableName)
                    .setColumn(STATUS_FIELD, literal("taken"))
                    .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id))
                    .ifColumn(STATUS_FIELD).isEqualTo(literal("scheduled"))
                    .build();

            try {
                boolean applied = cqlSession.execute(statement).wasApplied();

                if (!applied) {
                    scheduledJobs.remove(id).cancel(true);
                    return;
                }
                Process process = mappedProcesses.get(processId);
                if (process == null) {
                    LOGGER.warn("No process found for process id {}", processId);
                    return;
                }
                IdentityProvider.set(new TrustedIdentityProvider("System<timer>"));
                Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                        .add("message", "Executing timer job to create new workflow instance");

                auditor.publish(entry);
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
            } catch (QueryExecutionException rnf) {
                scheduledJobs.remove(id).cancel(true);
            }
        }
    }

}
