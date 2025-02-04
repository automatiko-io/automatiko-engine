package io.automatiko.engine.addons.persistence.dynamodb.job;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.config.DynamoDBJobsConfig;
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
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@ApplicationScoped
public class DynamoDBJobService implements JobsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBJobService.class);

    private static final String INSTANCE_ID_FIELD = "JobInstanceId";
    private static final String FIRE_AT_FIELD = "JobFireAt";
    private static final String OWNER_INSTANCE_ID_FIELD = "JobOwnerInstanceId";
    private static final String OWNER_DEF_ID_FIELD = "JobOwnerDefinitionId";
    private static final String TRIGGER_TYPE_FIELD = "JobTriggerType";
    private static final String STATUS_FIELD = "JobStatus";
    private static final String FIRE_LIMIT_FIELD = "JobFireLimit";
    private static final String REPEAT_INTERVAL_FIELD = "JobRepeatInterval";
    private static final String EXPRESSION_FIELD = "JobExpression";

    protected final DynamoDbClient dynamodb;

    protected final UnitOfWorkManager unitOfWorkManager;

    protected final Auditor auditor;

    protected final ScheduledThreadPoolExecutor scheduler;

    protected final ScheduledThreadPoolExecutor loadScheduler;

    protected Map<String, Process<? extends Model>> mappedProcesses = new HashMap<>();
    protected ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    protected final String tableName = "ATK_JOBS";

    private Optional<Boolean> createTables;

    private Optional<Long> readCapacity;

    private Optional<Long> writeCapacity;

    private Optional<Long> interval;

    private Optional<Integer> threads;

    @Inject
    public DynamoDBJobService(DynamoDbClient dynamodb,
            Processes processes, Application application, Auditor auditor,
            @ConfigProperty(name = "quarkus.automatiko.persistence.disabled") Optional<Boolean> persistenceDisabled,
            @ConfigProperty(name = DynamoDBJobsConfig.CREATE_TABLES_KEY) Optional<Boolean> createTables,
            @ConfigProperty(name = DynamoDBJobsConfig.READ_CAPACITY_KEY) Optional<Long> readCapacity,
            @ConfigProperty(name = DynamoDBJobsConfig.WRITE_CAPACITY_KEY) Optional<Long> writeCapacity,
            @ConfigProperty(name = DynamoDBJobsConfig.INTERVAL_KEY) Optional<Long> interval,
            @ConfigProperty(name = DynamoDBJobsConfig.THREADS_KEY) Optional<Integer> threads) {

        if (!persistenceDisabled.orElse(false)) {
            this.dynamodb = dynamodb;
            this.createTables = createTables;
            this.readCapacity = readCapacity;
            this.writeCapacity = writeCapacity;
            this.interval = interval;
            this.threads = threads;

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
            this.dynamodb = null;
            this.unitOfWorkManager = null;
            this.auditor = null;
            this.scheduler = null;
            this.loadScheduler = null;
        }
    }

    public void start(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER) StartupEvent event) {
        if (dynamodb != null) {
            loadScheduler.scheduleAtFixedRate(() -> {
                try {
                    long next = LocalDateTime.now().plus(Duration.ofMinutes(interval.orElse(10L)))
                            .atZone(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli();
                    Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
                    attrValues.put(":value", AttributeValue.builder().n(Long.toString(next)).build());
                    ScanRequest query = ScanRequest.builder().tableName(tableName)
                            .projectionExpression(INSTANCE_ID_FIELD + "," + FIRE_AT_FIELD + "," + OWNER_INSTANCE_ID_FIELD + ","
                                    + OWNER_DEF_ID_FIELD + "," +
                                    TRIGGER_TYPE_FIELD + "," + FIRE_LIMIT_FIELD + "," + REPEAT_INTERVAL_FIELD)
                            .filterExpression(FIRE_AT_FIELD + " < :value").expressionAttributeValues(attrValues).build();

                    List<Map<String, AttributeValue>> jobs = dynamodb.scan(query).items();
                    LOGGER.debug("Loaded jobs ({}) to be executed before {}", jobs.size(), next);
                    for (Map<String, AttributeValue> job : jobs) {

                        if (job.get(OWNER_INSTANCE_ID_FIELD) == null) {
                            ProcessJobDescription description = ProcessJobDescription.of(build(job.get(EXPRESSION_FIELD).s()),
                                    null,
                                    job.get(OWNER_DEF_ID_FIELD).s());

                            scheduledJobs.computeIfAbsent(job.get(INSTANCE_ID_FIELD).s(), k -> {
                                return log(job.get(INSTANCE_ID_FIELD).s(),
                                        scheduler.schedule(new StartProcessOnExpiredTimer(job.get(INSTANCE_ID_FIELD).s(),
                                                job.get(OWNER_DEF_ID_FIELD).s(), -1, description),
                                                Duration.between(LocalDateTime.now(),
                                                        ZonedDateTime.ofInstant(
                                                                Instant.ofEpochMilli(
                                                                        Long.parseLong(job.get(FIRE_AT_FIELD).n())),
                                                                ZoneId.systemDefault()))
                                                        .toMillis(),
                                                TimeUnit.MILLISECONDS));
                            });
                        } else {
                            ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(
                                    job.get(INSTANCE_ID_FIELD).s(),
                                    job.get(TRIGGER_TYPE_FIELD).s(),
                                    build(job.get(EXPRESSION_FIELD).s()), job.get(OWNER_INSTANCE_ID_FIELD).s(),
                                    job.get(OWNER_DEF_ID_FIELD).s(), null);

                            scheduledJobs.computeIfAbsent(job.get(INSTANCE_ID_FIELD).s(), k -> {
                                return log(job.get(INSTANCE_ID_FIELD).s(), scheduler.schedule(
                                        new SignalProcessInstanceOnExpiredTimer(job.get(INSTANCE_ID_FIELD).s(),
                                                job.get(TRIGGER_TYPE_FIELD).s(),
                                                job.get(OWNER_DEF_ID_FIELD).s(),
                                                job.get(OWNER_INSTANCE_ID_FIELD).s(),
                                                Integer.parseInt(job.get(FIRE_LIMIT_FIELD).n()), description),
                                        Duration.between(LocalDateTime.now(), ZonedDateTime.ofInstant(
                                                Instant.ofEpochMilli(Long.parseLong(job.get(FIRE_AT_FIELD).n())),
                                                ZoneId.systemDefault())).toMillis(),
                                        TimeUnit.MILLISECONDS));
                            });
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while loading jobs from dynamodb", e);
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
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        if (description.expirationTime().repeatInterval() != null) {
            itemValues.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(description.id()).build());
            itemValues.put(OWNER_DEF_ID_FIELD,
                    AttributeValue.builder().s(description.processId() + version(description.processVersion())).build());
            itemValues.put(STATUS_FIELD, AttributeValue.builder().s("scheduled").build());
            itemValues.put(FIRE_AT_FIELD,
                    AttributeValue.builder()
                            .n(Long.toString(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                            .build());
            itemValues.put(FIRE_LIMIT_FIELD,
                    AttributeValue.builder().n(Integer.toString(description.expirationTime().repeatLimit())).build());
            itemValues.put(REPEAT_INTERVAL_FIELD,
                    AttributeValue.builder().n(Long.toString(description.expirationTime().repeatInterval())).build());
            itemValues.put(EXPRESSION_FIELD,
                    AttributeValue.builder().s(nonNull(description.expirationTime().expression())).build());

            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled repeatable timer job that creates new workflow instances");

            auditor.publish(entry);
        } else {
            itemValues.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(description.id()).build());
            itemValues.put(OWNER_DEF_ID_FIELD,
                    AttributeValue.builder().s(description.processId() + version(description.processVersion())).build());
            itemValues.put(STATUS_FIELD, AttributeValue.builder().s("scheduled").build());
            itemValues.put(FIRE_AT_FIELD,
                    AttributeValue.builder()
                            .n(Long.toString(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                            .build());
            itemValues.put(FIRE_LIMIT_FIELD,
                    AttributeValue.builder().n(Integer.toString(description.expirationTime().repeatLimit() == null ? -1
                            : description.expirationTime().repeatLimit())).build());
            itemValues.put(EXPRESSION_FIELD,
                    AttributeValue.builder().s(nonNull(description.expirationTime().expression())).build());
            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled one time timer job that creates new workflow instances");

            auditor.publish(entry);
        }

        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(description.id()).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build();

        boolean exists = dynamodb.getItem(request).hasItem();
        if (!exists) {
            PutItemRequest requestPut = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(itemValues)
                    .build();

            dynamodb.putItem(requestPut);
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

        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        if (description.expirationTime().repeatInterval() != null) {
            itemValues.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(description.id()).build());
            itemValues.put(TRIGGER_TYPE_FIELD, AttributeValue.builder().s(description.triggerType()).build());
            itemValues.put(OWNER_DEF_ID_FIELD,
                    AttributeValue.builder().s(description.processId() + version(description.processVersion())).build());
            itemValues.put(OWNER_INSTANCE_ID_FIELD, AttributeValue.builder().s(description.processInstanceId()).build());
            itemValues.put(STATUS_FIELD, AttributeValue.builder().s("scheduled").build());
            itemValues.put(FIRE_AT_FIELD,
                    AttributeValue.builder()
                            .n(Long.toString(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                            .build());
            itemValues.put(FIRE_LIMIT_FIELD,
                    AttributeValue.builder().n(Integer.toString(description.expirationTime().repeatLimit())).build());
            itemValues.put(REPEAT_INTERVAL_FIELD,
                    AttributeValue.builder().n(Long.toString(description.expirationTime().repeatInterval())).build());
            itemValues.put(EXPRESSION_FIELD,
                    AttributeValue.builder().s(nonNull(description.expirationTime().expression())).build());

            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled repeatable timer job for existing workflow instance");

            auditor.publish(entry);

        } else {
            itemValues.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(description.id()).build());
            itemValues.put(TRIGGER_TYPE_FIELD, AttributeValue.builder().s(description.triggerType()).build());
            itemValues.put(OWNER_DEF_ID_FIELD,
                    AttributeValue.builder().s(description.processId() + version(description.processVersion())).build());
            itemValues.put(OWNER_INSTANCE_ID_FIELD, AttributeValue.builder().s(description.processInstanceId()).build());
            itemValues.put(STATUS_FIELD, AttributeValue.builder().s("scheduled").build());
            itemValues.put(FIRE_AT_FIELD,
                    AttributeValue.builder()
                            .n(Long.toString(description.expirationTime().get().toLocalDateTime().atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()))
                            .build());
            itemValues.put(FIRE_LIMIT_FIELD,
                    AttributeValue.builder().n(Integer.toString(description.expirationTime().repeatLimit() == null ? -1
                            : description.expirationTime().repeatLimit())).build());
            itemValues.put(EXPRESSION_FIELD,
                    AttributeValue.builder().s(nonNull(description.expirationTime().expression())).build());

            Supplier<AuditEntry> entry = () -> BaseAuditEntry.timer(description)
                    .add("message", "Scheduled one time timer job for existing workflow instance");

            auditor.publish(entry);
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build();

        dynamodb.putItem(request);

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
        Supplier<AuditEntry> entry = () -> {
            Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

            keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

            GetItemRequest request = GetItemRequest.builder()
                    .key(keyToGet)
                    .tableName(tableName)
                    .projectionExpression(INSTANCE_ID_FIELD + "," + EXPRESSION_FIELD + "," + OWNER_INSTANCE_ID_FIELD + ","
                            + OWNER_DEF_ID_FIELD + "," +
                            TRIGGER_TYPE_FIELD + "," + FIRE_LIMIT_FIELD + "," + REPEAT_INTERVAL_FIELD)
                    .build();

            Map<String, AttributeValue> returnedItem = dynamodb.getItem(request).item();

            if (returnedItem != null) {
                return BaseAuditEntry.timer()
                        .add("message", "Cancelled job for existing workflow instance")
                        .add("jobId", id)
                        .add("timerExpression",
                                returnedItem.get(EXPRESSION_FIELD) != null ? returnedItem.get(EXPRESSION_FIELD).n() : null)
                        .add("timerInterval",
                                returnedItem.get(REPEAT_INTERVAL_FIELD) != null ? returnedItem.get(REPEAT_INTERVAL_FIELD).n()
                                        : null)
                        .add("timerRepeatLimit",
                                returnedItem.get(FIRE_LIMIT_FIELD) != null ? returnedItem.get(FIRE_LIMIT_FIELD).n() : null)
                        .add("workflowDefinitionId", returnedItem.get(OWNER_DEF_ID_FIELD).n())
                        .add("workflowInstanceId", returnedItem.get(OWNER_INSTANCE_ID_FIELD).n())
                        .add("triggerType", TRIGGER_TYPE_FIELD);
            } else {
                return BaseAuditEntry.timer()
                        .add("message", "Cancelled job for existing workflow instance")
                        .add("jobId", id);
            }
        };

        auditor.publish(entry);
        removeScheduledJob(id);

        return true;
    }

    @Override
    public ZonedDateTime getScheduledTime(String id) {
        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build();

        Map<String, AttributeValue> returnedItem = dynamodb.getItem(request).item();

        if (returnedItem != null) {
            Long fireAt = Long.valueOf(returnedItem.get(FIRE_AT_FIELD).n());

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
        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder()
                .s(id)
                .build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keyToGet)
                .build();

        dynamodb.deleteItem(deleteReq);
    }

    protected void updateRepeatableJob(String id) {
        HashMap<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();
        itemKey.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

        GetItemRequest getrequest = GetItemRequest.builder()
                .key(itemKey)
                .tableName(tableName)
                .build();

        Map<String, AttributeValue> job = dynamodb.getItem(getrequest).item();

        Integer limit = Integer.parseInt(job.get(FIRE_LIMIT_FIELD).n()) - 1;
        Long repeat = Long.parseLong(job.get(REPEAT_INTERVAL_FIELD).n());
        ZonedDateTime fireTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(Long.parseLong(job.get(FIRE_AT_FIELD).n())),
                ZoneId.systemDefault());

        Map<String, AttributeValueUpdate> updatedValues = new HashMap<String, AttributeValueUpdate>();
        updatedValues.put(FIRE_AT_FIELD, AttributeValueUpdate.builder()
                .value(AttributeValue.builder()
                        .n(Long.toString(fireTime.plus(repeat, ChronoUnit.MILLIS).toInstant().toEpochMilli())).build())
                .action(AttributeAction.PUT)
                .build());
        updatedValues.put(FIRE_LIMIT_FIELD, AttributeValueUpdate.builder()
                .value(AttributeValue.builder()
                        .n(Integer.toString(limit)).build())
                .action(AttributeAction.PUT)
                .build());
        updatedValues.put(STATUS_FIELD, AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("scheduled").build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .build();

        dynamodb.updateItem(request);

        if (job.get(OWNER_INSTANCE_ID_FIELD) == null) {
            ProcessJobDescription description = ProcessJobDescription.of(build(job.get(EXPRESSION_FIELD).s()), null,
                    job.get(OWNER_DEF_ID_FIELD).s());
            scheduledJobs.computeIfAbsent(job.get(INSTANCE_ID_FIELD).s(), k -> {
                return log(job.get(INSTANCE_ID_FIELD).s(),
                        scheduler.schedule(new StartProcessOnExpiredTimer(job.get(INSTANCE_ID_FIELD).s(),
                                job.get(OWNER_DEF_ID_FIELD).s(), limit, description),
                                Duration.between(LocalDateTime.now(), fireTime).toMillis(),
                                TimeUnit.MILLISECONDS));
            });
        } else {
            ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(job.get(INSTANCE_ID_FIELD).s(),
                    job.get(TRIGGER_TYPE_FIELD).s(),
                    build(job.get(EXPRESSION_FIELD).s()), job.get(OWNER_INSTANCE_ID_FIELD).s(),
                    job.get(OWNER_DEF_ID_FIELD).s(), null);

            scheduledJobs.computeIfAbsent(job.get(INSTANCE_ID_FIELD).s(), k -> {
                return log(job.get(INSTANCE_ID_FIELD).s(), scheduler.scheduleAtFixedRate(
                        new SignalProcessInstanceOnExpiredTimer(job.get(INSTANCE_ID_FIELD).s(),
                                job.get(TRIGGER_TYPE_FIELD).s(),
                                job.get(OWNER_DEF_ID_FIELD).s(),
                                job.get(OWNER_INSTANCE_ID_FIELD).s(), limit, description),
                        Duration.between(LocalDateTime.now(), fireTime).toMillis(), repeat,
                        TimeUnit.MILLISECONDS));
            });
        }
    }

    protected ScheduledFuture<?> log(String jobId, ScheduledFuture<?> future) {
        LOGGER.debug("Next fire of job {} is in {} seconds ", jobId, future.getDelay(TimeUnit.SECONDS));

        return future;
    }

    protected ExpirationTime build(String expression) {
        if (expression != null && !expression.equals("notset")) {
            return CronExpirationTime.of(expression);
        }

        return new NoOpExpirationTime();
    }

    protected String nonNull(String expression) {
        if (expression != null) {
            return expression;
        }

        return "notset";
    }

    protected void createTable() {
        DynamoDbWaiter dbWaiter = dynamodb.waiter();

        List<KeySchemaElement> indexKeySchema = new ArrayList<KeySchemaElement>();
        indexKeySchema.add(KeySchemaElement.builder().attributeName(INSTANCE_ID_FIELD).keyType(KeyType.HASH).build());

        CreateTableRequest request = CreateTableRequest.builder()
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(INSTANCE_ID_FIELD).attributeType(ScalarAttributeType.S)
                                .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(INSTANCE_ID_FIELD)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(readCapacity.orElse(Long.valueOf(10)))
                        .writeCapacityUnits(writeCapacity.orElse(Long.valueOf(10)))
                        .build())
                .tableName(tableName)
                .build();

        try {
            CreateTableResponse response = dynamodb.createTable(request);
            if (response.sdkHttpResponse().isSuccessful()) {
                DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                        .tableName(tableName)
                        .build();

                // Wait until the Amazon DynamoDB table is created
                WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
                waiterResponse.matched().response()
                        .ifPresent(r -> LOGGER.debug("Table for jobs created in DynamoDB {}", r.toString()));
            } else {
                throw new RuntimeException("Unable to create table for jobs reason "
                        + response.sdkHttpResponse().statusText());
            }
        } catch (ResourceInUseException e) {
            // ignore as this means table exists
        } catch (DynamoDbException e) {
            throw new RuntimeException("Unable to create table for jobs", e);
        }
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

            Map<String, AttributeValue> key = new HashMap<>();
            key.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

            Map<String, AttributeValue> productItemKey = new HashMap<>();
            productItemKey.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":new_status", AttributeValue.builder().s("taken").build());
            expressionAttributeValues.put(":expected_status", AttributeValue.builder().s("scheduled").build());

            Update markItemSold = Update.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET " + STATUS_FIELD + " = :new_status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .conditionExpression(STATUS_FIELD + " = :expected_status")
                    .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD).build();

            Collection<TransactWriteItem> actions = Arrays.asList(
                    TransactWriteItem.builder().update(markItemSold).build());

            TransactWriteItemsRequest placeOrderTransaction = TransactWriteItemsRequest.builder()
                    .transactItems(actions)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).build();

            try {
                dynamodb.transactWriteItems(placeOrderTransaction);

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
            } catch (ResourceNotFoundException rnf) {
                scheduledJobs.remove(id).cancel(true);
            } catch (TransactionCanceledException tce) {
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
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

            Map<String, AttributeValue> productItemKey = new HashMap<>();
            productItemKey.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(id).build());

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":new_status", AttributeValue.builder().s("taken").build());
            expressionAttributeValues.put(":expected_status", AttributeValue.builder().s("scheduled").build());

            Update markItemSold = Update.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET " + STATUS_FIELD + " = :new_status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .conditionExpression(STATUS_FIELD + " = :expected_status")
                    .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD).build();

            Collection<TransactWriteItem> actions = Arrays.asList(
                    TransactWriteItem.builder().update(markItemSold).build());

            TransactWriteItemsRequest placeOrderTransaction = TransactWriteItemsRequest.builder()
                    .transactItems(actions)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).build();

            try {
                dynamodb.transactWriteItems(placeOrderTransaction);
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
            } catch (ResourceNotFoundException rnf) {
                scheduledJobs.remove(id).cancel(true);
            } catch (TransactionCanceledException tce) {
                scheduledJobs.remove(id).cancel(true);
            }
        }
    }

}
