package io.automatiko.engine.addons.persistence.cassandra;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.deleteFrom;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createKeyspace;
import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.data.ByteUtils;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.addons.persistence.common.tlog.TransactionLogImpl;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.uow.TransactionLogStore;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CassandraProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraProcessInstances.class);

    private static final String INSTANCE_ID_FIELD = "InstanceId";
    private static final String CONTENT_FIELD = "Content";
    private static final String TAGS_FIELD = "Tags";
    private static final String VERSION_FIELD = "VersionTrack";
    private static final String STATUS_FIELD = "PIStatus";
    private static final String START_DATE_FIELD = "PIStartDate";
    private static final String END_DATE_FIELD = "PIEndDate";
    private static final String EXPIRED_AT_FIELD = "PIExpiredAtDate";

    private final Process<? extends Model> process;
    private final ProcessInstanceMarshaller marshaller;
    private final StoredDataCodec codec;

    private CqlSession cqlSession;

    private String tableName;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    private TransactionLog transactionLog;

    private Auditor auditor;

    private Optional<Boolean> createKeyspace;

    private Optional<Boolean> createTables;

    private Optional<String> keyspace;

    public CassandraProcessInstances(Process<? extends Model> process, CqlSession cqlSession,
            StoredDataCodec codec, TransactionLogStore store, Auditor auditor,
            Optional<Boolean> createKeyspace, Optional<Boolean> createTables, Optional<String> keyspace) {
        this.process = process;
        this.marshaller = new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy(process));
        this.cqlSession = cqlSession;
        this.tableName = process.id().toUpperCase();
        this.codec = codec;
        this.auditor = auditor;
        this.createKeyspace = createKeyspace;
        this.createTables = createTables;
        this.keyspace = keyspace;

        if (this.createTables.orElse(Boolean.TRUE)) {
            createTable();
        }

        this.transactionLog = new TransactionLogImpl(store, new JacksonObjectMarshallingStrategy(process));
    }

    @Override
    public TransactionLog transactionLog() {
        return transactionLog;
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

        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(CONTENT_FIELD)
                .column(VERSION_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(resolvedId))
                .whereColumn(STATUS_FIELD).isEqualTo(literal(status));
        if (status == ProcessInstance.STATE_RECOVERING) {
            byte[] content = this.transactionLog.readContent(process.id(), resolvedId);

            // transaction log found value but not in the cassandra storage so use it as it is part of recovery
            if (content != null) {
                long versionTracker = 1;
                ResultSet rs = cqlSession.execute(select.build());
                Row row = rs.one();
                if (row != null) {
                    versionTracker = row.getLong(VERSION_FIELD);
                }
                return Optional
                        .of(audit(mode == MUTABLE || mode == ProcessInstanceReadMode.MUTABLE_WITH_LOCK
                                ? marshaller.unmarshallProcessInstance(content, process, versionTracker)
                                : marshaller.unmarshallReadOnlyProcessInstance(content, process)));
            }
        }

        ResultSet rs = cqlSession.execute(select.build());
        Row row = rs.one();
        if (row != null) {
            byte[] content = ByteUtils.getArray(row.getByteBuffer(CONTENT_FIELD));

            return Optional
                    .of(audit(mode == MUTABLE || mode == ProcessInstanceReadMode.MUTABLE_WITH_LOCK
                            ? marshaller.unmarshallProcessInstance(codec.decode(content), process, row.getLong(VERSION_FIELD))
                            : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process)));

        } else {
            return Optional.empty();
        }

    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size) {
        LOGGER.debug("values() called");
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(CONTENT_FIELD)
                .column(VERSION_FIELD).whereColumn(STATUS_FIELD).isEqualTo(literal(status));

        ResultSet rs = cqlSession.execute(select.build());

        return rs.all().stream().map(item -> {
            try {
                byte[] content = ByteUtils.getArray(item.getByteBuffer(CONTENT_FIELD));

                return audit(mode == MUTABLE || mode == ProcessInstanceReadMode.MUTABLE_WITH_LOCK
                        ? marshaller.unmarshallProcessInstance(codec.decode(content), process, item.getLong(VERSION_FIELD))
                        : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process));
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
        LOGGER.debug("findByIdOrTag() called for values {} and status {}", values, status);

        List<Row> collected = new ArrayList<Row>();
        Set<String> distinct = new HashSet<String>();

        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
                .column(CONTENT_FIELD)
                .column(VERSION_FIELD)
                .column(TAGS_FIELD)
                .whereColumn(STATUS_FIELD).isEqualTo(literal(status));

        ResultSet rs = cqlSession.execute(select.build());
        rs.all().stream().filter(r -> !distinct.contains(r.getString(INSTANCE_ID_FIELD)))
                .filter(r -> {
                    if (values == null || values.length == 0) {
                        return true;
                    }

                    Set<String> tags = r.getSet(TAGS_FIELD, String.class);
                    return Stream.of(values).anyMatch(v -> tags.contains(v));
                })
                .forEach(r -> {
                    distinct.add(r.getString(INSTANCE_ID_FIELD));
                    collected.add(r);
                });

        return collected.stream().map(item -> {
            try {
                byte[] content = ByteUtils.getArray(item.getByteBuffer(CONTENT_FIELD));

                return audit(mode == MUTABLE || mode == ProcessInstanceReadMode.MUTABLE_WITH_LOCK
                        ? marshaller.unmarshallProcessInstance(codec.decode(content), process, item.getLong(VERSION_FIELD))
                        : marshaller.unmarshallReadOnlyProcessInstance(codec.decode(content), process));
            } catch (AccessDeniedException e) {
                return null;
            }
        })
                .filter(pi -> pi != null)
                .collect(Collectors.toList());

    }

    @Override
    public Collection locateByIdOrTag(int status, String... values) {
        LOGGER.debug("locateByIdOrTag() called for values {} and status {}", values, status);

        Set<String> distinct = new HashSet<String>();

        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
                .column(TAGS_FIELD)
                .whereColumn(STATUS_FIELD).isEqualTo(literal(status));

        ResultSet rs = cqlSession.execute(select.build());
        rs.all().stream().filter(r -> !distinct.contains(r.getString(INSTANCE_ID_FIELD)))
                .filter(r -> {
                    if (values == null || values.length == 0) {
                        return true;
                    }

                    Set<String> tags = r.getSet(TAGS_FIELD, String.class);
                    return Stream.of(values).anyMatch(v -> tags.contains(v));
                })
                .forEach(r -> {
                    distinct.add(r.getString(INSTANCE_ID_FIELD));

                });

        return distinct;
    }

    @Override
    public Long size() {
        LOGGER.debug("size() called");
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName)
                .countAll();

        ResultSet rs = cqlSession.execute(select.build().setConsistencyLevel(ConsistencyLevel.LOCAL_ONE));
        Row row = rs.one();

        return row.getLong(0);
    }

    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return true;
        }
        LOGGER.debug("exists() called for instance {}", resolvedId);
        Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(id));

        ResultSet rs = cqlSession.execute(select.build());
        Row row = rs.one();
        if (row != null) {
            return true;
        }

        return false;

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

            Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
            tags.add(resolvedId);
            if (instance.businessKey() != null) {
                tags.add(instance.businessKey());
            }

            Insert insert = insertInto(keyspace.orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(resolvedId))
                    .value(VERSION_FIELD, literal(((AbstractProcessInstance<?>) instance).getVersionTracker()))
                    .value(STATUS_FIELD, literal(((AbstractProcessInstance<?>) instance).status()))
                    .value(CONTENT_FIELD, bindMarker())
                    .value(START_DATE_FIELD, literal(instance.startDate().toInstant()))
                    .value(END_DATE_FIELD,
                            literal(instance.endDate() == null ? null
                                    : instance.endDate().toInstant()))
                    .value(EXPIRED_AT_FIELD,
                            literal(instance.expiresAtDate() == null ? null
                                    : instance.expiresAtDate().toInstant()))
                    .value(TAGS_FIELD, bindMarker()).ifNotExists();

            try {
                ResultSet rs = cqlSession.execute(cqlSession.prepare(insert.build()).bind(ByteBuffer.wrap(data), tags));
                if (!rs.wasApplied()) {
                    throw new ProcessInstanceDuplicatedException(id);
                }
                Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                        .add("message", "Workflow instance created in the Apache Cassandra based data store");

                auditor.publish(entry);
            } catch (QueryExecutionException e) {
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

        LOGGER.debug("update() called for instance {}", resolvedId);
        byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));
        if (data == null) {
            return;
        }

        Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
        tags.add(resolvedId);
        if (instance.businessKey() != null) {
            tags.add(instance.businessKey());
        }

        SimpleStatement statement = QueryBuilder.update(keyspace.orElse("automatiko"), tableName)
                .setColumn(CONTENT_FIELD, bindMarker())
                .setColumn(TAGS_FIELD, bindMarker())
                .setColumn(VERSION_FIELD, literal(((AbstractProcessInstance<?>) instance).getVersionTracker() + 1))
                .setColumn(STATUS_FIELD, literal(((AbstractProcessInstance<?>) instance).status()))
                .setColumn(START_DATE_FIELD, literal(instance.startDate().toInstant()))
                .setColumn(END_DATE_FIELD,
                        literal(instance.endDate() == null ? null
                                : instance.endDate().toInstant()))
                .setColumn(EXPIRED_AT_FIELD, literal(instance.expiresAtDate() == null ? null
                        : instance.expiresAtDate().toInstant()))
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(resolvedId))
                .ifColumn(VERSION_FIELD).isEqualTo(literal(((AbstractProcessInstance<?>) instance).getVersionTracker()))
                .build();

        ResultSet rs = cqlSession.execute(cqlSession.prepare(statement)
                .bind(ByteBuffer.wrap(data), tags));
        if (!rs.wasApplied()) {
            if (transactionLog.contains(process.id(), instance.id())) {
                Insert insert = insertInto(keyspace.orElse("automatiko"), tableName)
                        .value(INSTANCE_ID_FIELD, literal(resolvedId))
                        .value(VERSION_FIELD, literal(((AbstractProcessInstance<?>) instance).getVersionTracker()))
                        .value(STATUS_FIELD, literal(((AbstractProcessInstance<?>) instance).status()))
                        .value(CONTENT_FIELD, bindMarker())
                        .value(START_DATE_FIELD, literal(instance.startDate().toInstant()))
                        .value(END_DATE_FIELD,
                                literal(instance.endDate() == null ? null
                                        : instance.endDate().toInstant()))
                        .value(EXPIRED_AT_FIELD, literal(instance.expiresAtDate() == null ? null
                                : instance.expiresAtDate()))
                        .value(TAGS_FIELD, bindMarker()).ifNotExists();

                try {
                    rs = cqlSession.execute(cqlSession.prepare(insert.build()).bind(ByteBuffer.wrap(data), tags));
                    if (!rs.wasApplied()) {
                        throw new ProcessInstanceDuplicatedException(id);
                    }
                    Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                            .add("message", "Workflow instance updated in the Apache Cassandra based data store");

                    auditor.publish(entry);
                } catch (QueryExecutionException e) {
                    throw new ProcessInstanceDuplicatedException(id);
                }
            } else {
                throw new ConflictingVersionException("Process instance with id '" + instance.id()
                        + "' has older version than the stored one");
            }
        }

        disconnect(instance);

        cachedInstances.remove(resolvedId);

    }

    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        LOGGER.debug("remove() called for instance {}", resolvedId);
        cachedInstances.remove(resolvedId);
        cachedInstances.remove(id);

        Delete deleteStatement = deleteFrom(keyspace.orElse("automatiko"), tableName).whereColumn(INSTANCE_ID_FIELD)
                .isEqualTo(literal(resolvedId)).ifExists();

        cqlSession.execute(deleteStatement.build());
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                .add("message", "Workflow instance removed from the Apache Cassandra based data store");

        auditor.publish(entry);
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
                .withColumn(STATUS_FIELD, DataTypes.INT)
                .withColumn(CONTENT_FIELD, DataTypes.BLOB)
                .withColumn(TAGS_FIELD, DataTypes.setOf(DataTypes.TEXT))
                .withColumn(VERSION_FIELD, DataTypes.BIGINT)
                .withColumn(START_DATE_FIELD, DataTypes.TIMESTAMP)
                .withColumn(END_DATE_FIELD, DataTypes.TIMESTAMP)
                .withColumn(EXPIRED_AT_FIELD, DataTypes.TIMESTAMP);

        cqlSession.execute(createTable.build());

        CreateIndex index = SchemaBuilder.createIndex(tableName + "_STATUS_IDX").ifNotExists()
                .onTable(keyspace.orElse("automatiko"), tableName).andColumn(STATUS_FIELD);
        cqlSession.execute(index.build());
    }

    protected void disconnect(ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                Select select = selectFrom(keyspace.orElse("automatiko"), tableName).column(CONTENT_FIELD)
                        .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(resolveId(instance.id(), instance)));

                ResultSet rs = cqlSession.execute(select.build());
                Row row = rs.one();
                if (row != null) {

                    byte[] reloaded = codec.decode(row.getByteBuffer(1).array());

                    return marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
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

        ExportedProcessInstance exported = marshaller.exportProcessInstance(audit(instance));

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

    public ProcessInstance<?> audit(ProcessInstance<?> instance) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                .add("message", "Workflow instance was read from the Apache Cassandra based data store");

        auditor.publish(entry);

        return instance;
    }

}
