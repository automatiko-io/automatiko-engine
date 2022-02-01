package io.automatiko.engine.addons.persistence.dynamodb;

import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.config.DynamoDBPersistenceConfig;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
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
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class DynamoDBProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBProcessInstances.class);

    private static final String INSTANCE_ID_FIELD = "InstanceId";
    private static final String CONTENT_FIELD = "Content";
    private static final String TAGS_FIELD = "Tags";
    private static final String VERSION_FIELD = "VersionTrack";
    private static final String STATUS_FIELD = "PIStatus";

    private final Process<? extends Model> process;
    private final ProcessInstanceMarshaller marshaller;
    private final StoredDataCodec codec;

    private final DynamoDBPersistenceConfig config;

    private DynamoDbClient dynamodb;

    private String tableName;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public DynamoDBProcessInstances(Process<? extends Model> process, DynamoDbClient dynamodb,
            DynamoDBPersistenceConfig config, StoredDataCodec codec) {
        this.process = process;
        this.marshaller = new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy(process));
        this.config = config;
        this.dynamodb = dynamodb;
        this.tableName = process.id().toUpperCase();
        this.codec = codec;

        if (config.createTables().orElse(Boolean.TRUE)) {
            createTable();
        }
    }

    @Override
    public Optional<? extends ProcessInstance> findById(String id, int status, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return Optional.of(cachedInstances.get(resolvedId));
        }
        if (resolvedId.contains(":")) {
            if (cachedInstances.containsKey(resolvedId.split(":")[1])) {
                ProcessInstance pi = cachedInstances.get(resolvedId.split(":")[1]);
                if (pi.status() == status) {
                    return Optional.of(pi);
                } else {
                    return Optional.empty();
                }
            }
        }
        LOGGER.debug("findById() called for instance {}", resolvedId);
        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(resolvedId).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build();

        Map<String, AttributeValue> returnedItem = dynamodb.getItem(request).item();

        if (returnedItem != null && Integer.parseInt(returnedItem.get(STATUS_FIELD).n()) == status) {
            byte[] content = returnedItem.get(CONTENT_FIELD).b().asByteArray();

            return Optional.of(mode == MUTABLE
                    ? marshaller.unmarshallProcessInstance(codec.decode(content), process,
                            Long.parseLong(returnedItem.get(VERSION_FIELD).n()))
                    : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process));

        } else {
            return Optional.empty();
        }

    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size) {
        LOGGER.debug("values() called");
        Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
        StringBuilder condition = new StringBuilder();
        attrValues.put(":status", AttributeValue.builder().n(String.valueOf(status)).build());
        condition.append(STATUS_FIELD + " = :status ");

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression(condition.toString())
                .expressionAttributeValues(attrValues)
                .limit(page * size)
                .build();

        return dynamodb.scanPaginator(request).items().stream().map(item -> {
            try {
                byte[] content = item.get(CONTENT_FIELD).b().asByteArray();

                return mode == MUTABLE ? marshaller.unmarshallProcessInstance(codec.decode(content), process,
                        Long.parseLong(item.get(VERSION_FIELD).n()))
                        : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process);
            } catch (AccessDeniedException e) {
                return null;
            }
        })
                .filter(pi -> pi != null)
                .skip(calculatePage(page, size))
                .limit(size)
                .collect(Collectors.toList());

    }

    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values) {
        LOGGER.debug("findByIdOrTag() called for values {}", values);
        Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
        int counter = 0;
        StringBuilder condition = new StringBuilder();
        attrValues.put(":status", AttributeValue.builder().n(String.valueOf(status)).build());
        condition.append(STATUS_FIELD + "= :status AND ");
        for (String value : values) {
            attrValues.put(":value" + counter, AttributeValue.builder().s(value).build());
            condition.append("contains(" + TAGS_FIELD + ", :value" + counter + ") OR ");
            counter++;
        }
        condition.delete(condition.length() - 4, condition.length());

        ScanRequest query = ScanRequest.builder().tableName(tableName)
                .filterExpression(condition.toString())
                .expressionAttributeValues(attrValues).build();

        return dynamodb.scan(query).items().stream().map(item -> {
            try {
                byte[] content = item.get(CONTENT_FIELD).b().asByteArray();

                return mode == MUTABLE ? marshaller.unmarshallProcessInstance(codec.decode(content), process,
                        Long.parseLong(item.get(VERSION_FIELD).n()))
                        : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process);
            } catch (AccessDeniedException e) {
                return null;
            }
        })
                .filter(pi -> pi != null)
                .collect(Collectors.toList());

    }

    @Override
    public Collection locateByIdOrTag(int status, String... values) {
        Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
        int counter = 0;
        StringBuilder condition = new StringBuilder();
        attrValues.put(":status", AttributeValue.builder().n(String.valueOf(status)).build());
        condition.append(STATUS_FIELD + "= :status AND ");
        for (String value : values) {
            attrValues.put(":value" + counter, AttributeValue.builder().s(value).build());
            condition.append("contains(" + TAGS_FIELD + ", :value" + counter + ") OR ");
            counter++;
        }
        condition.delete(condition.length() - 4, condition.length());

        ScanRequest query = ScanRequest.builder().tableName(tableName)
                .filterExpression(condition.toString())
                .expressionAttributeValues(attrValues).build();

        return dynamodb.scan(query).items().stream().map(item -> {
            return item.get(INSTANCE_ID_FIELD).s();

        })
                .collect(Collectors.toSet());
    }

    @Override
    public Long size() {
        LOGGER.debug("size() called");
        ScanRequest query = ScanRequest.builder().tableName(tableName).select(Select.COUNT).build();
        return dynamodb.scan(query).count().longValue();
    }

    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return true;
        }
        LOGGER.debug("exists() called for instance {}", resolvedId);
        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(resolvedId).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build();

        return dynamodb.getItem(request).hasItem();

    }

    @Override
    public void create(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);

        if (isActive(instance)) {
            LOGGER.debug("create() called for instance {}", resolvedId);
            byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));
            if (data == null) {
                return;
            }

            Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
            itemValues.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(resolvedId).build());
            itemValues.put(VERSION_FIELD, AttributeValue.builder()
                    .n(String.valueOf(((AbstractProcessInstance<?>) instance).getVersionTracker())).build());
            itemValues.put(STATUS_FIELD, AttributeValue.builder()
                    .n(String.valueOf(((AbstractProcessInstance<?>) instance).status())).build());
            itemValues.put(CONTENT_FIELD, AttributeValue.builder().b(SdkBytes.fromByteArray(data)).build());

            Collection<String> tags = new ArrayList(instance.tags().values());
            tags.add(resolvedId);
            if (instance.businessKey() != null) {
                tags.add(instance.businessKey());
            }
            itemValues.put(TAGS_FIELD, AttributeValue.builder().ss(tags).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .conditionExpression("attribute_not_exists(" + INSTANCE_ID_FIELD + ")")
                    .item(itemValues)
                    .build();

            try {
                dynamodb.putItem(request);
            } catch (ConditionalCheckFailedException e) {
                throw new ProcessInstanceDuplicatedException(id);
            } finally {

                cachedInstances.remove(resolvedId);
                cachedInstances.remove(id);

                disconnect(instance);

            }
        } else if (isPending(instance)) {
            if (cachedInstances.putIfAbsent(resolvedId, instance) != null) {
                throw new ProcessInstanceDuplicatedException(id);
            }
        } else {
            cachedInstances.remove(resolvedId);
            cachedInstances.remove(id);
        }
    }

    @Override
    public void update(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);

        if (isActive(instance)) {
            LOGGER.debug("update() called for instance {}", resolvedId);
            byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));
            if (data == null) {
                return;
            }
            HashMap<String, AttributeValue> itemKey = new HashMap<String, AttributeValue>();

            itemKey.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(resolvedId).build());

            Map<String, AttributeValueUpdate> updatedValues = new HashMap<String, AttributeValueUpdate>();
            updatedValues.put(CONTENT_FIELD, AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().b(SdkBytes.fromByteArray(data)).build())
                    .action(AttributeAction.PUT)
                    .build());

            updatedValues.put(VERSION_FIELD, AttributeValueUpdate.builder()
                    .value(AttributeValue.builder()
                            .n(String.valueOf(((AbstractProcessInstance<?>) instance).getVersionTracker() + 1)).build())
                    .action(AttributeAction.PUT)
                    .build());

            updatedValues.put(STATUS_FIELD, AttributeValueUpdate.builder()
                    .value(AttributeValue.builder()
                            .n(String.valueOf(((AbstractProcessInstance<?>) instance).status())).build())
                    .action(AttributeAction.PUT)
                    .build());

            Collection<String> tags = new ArrayList(instance.tags().values());
            tags.add(resolvedId);
            if (instance.businessKey() != null) {
                tags.add(instance.businessKey());
            }
            updatedValues.put(TAGS_FIELD, AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().ss(tags).build())
                    .action(AttributeAction.PUT)
                    .build());

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(itemKey)
                    .attributeUpdates(updatedValues)
                    .conditionExpression(VERSION_FIELD + " = " + ((AbstractProcessInstance<?>) instance).getVersionTracker())
                    .build();

            try {
                dynamodb.updateItem(request);
            } catch (ConditionalCheckFailedException e) {
                throw new ConflictingVersionException("Process instance with id '" + instance.id()
                        + "' has older version than the stored one");
            } finally {
                disconnect(instance);
            }

        }
        cachedInstances.remove(resolvedId);

    }

    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        LOGGER.debug("remove() called for instance {}", resolvedId);
        cachedInstances.remove(resolvedId);
        cachedInstances.remove(id);

        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

        keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder()
                .s(resolvedId)
                .build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keyToGet)
                .build();

        dynamodb.deleteItem(deleteReq);
    }

    protected void createTable() {
        DynamoDbWaiter dbWaiter = dynamodb.waiter();
        CreateTableRequest request = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(INSTANCE_ID_FIELD)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(INSTANCE_ID_FIELD)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(config.readCapacity().orElse(Long.valueOf(10)))
                        .writeCapacityUnits(config.writeCapacity().orElse(Long.valueOf(10)))
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
                        .ifPresent(
                                r -> LOGGER.debug("Table for process {} created in DynamoDB {}", process.id(), r.toString()));
            } else {
                throw new RuntimeException("Unable to create table for process " + process.id() + " reason "
                        + response.sdkHttpResponse().statusText());
            }
        } catch (ResourceInUseException e) {
            // ignore as this means table exists
        } catch (DynamoDbException e) {
            throw new RuntimeException("Unable to create table for process " + process.id(), e);
        }
    }

    protected void disconnect(ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

                keyToGet.put(INSTANCE_ID_FIELD, AttributeValue.builder().s(instance.id()).build());

                GetItemRequest request = GetItemRequest.builder()
                        .key(keyToGet)
                        .tableName(tableName)
                        .build();

                Map<String, AttributeValue> returnedItem = dynamodb.getItem(request).item();

                if (returnedItem != null) {
                    byte[] reloaded = returnedItem.get(CONTENT_FIELD).b().asByteArray();

                    return marshaller.unmarshallWorkflowProcessInstance(codec.decode(reloaded), process);
                } else {
                    return null;
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }

        });
    }

    @Override
    public ExportedProcessInstance exportInstance(ProcessInstance instance, boolean abort) {

        ExportedProcessInstance exported = marshaller.exportProcessInstance(instance);

        if (abort) {
            instance.abort();
        }

        return exported;

    }

    @Override
    public ProcessInstance importInstance(ExportedProcessInstance instance, Process process) {
        ProcessInstance imported = marshaller.importProcessInstance(instance, process);

        if (exists(imported.id())) {
            throw new ProcessInstanceDuplicatedException(imported.id());
        }

        create(imported.id(), imported);
        return imported;
    }

}
