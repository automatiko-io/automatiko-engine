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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.config.CassandraPersistenceConfig;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceNotFoundException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CassandraProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraProcessInstances.class);

    private static final String INSTANCE_ID_FIELD = "InstanceId";
    private static final String CONTENT_FIELD = "Content";
    private static final String TAGS_FIELD = "Tags";

    private final Process<? extends Model> process;
    private final ProcessInstanceMarshaller marshaller;

    private final CassandraPersistenceConfig config;

    private CqlSession cqlSession;

    private String tableName;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public CassandraProcessInstances(Process<? extends Model> process, CqlSession cqlSession,
            CassandraPersistenceConfig config) {
        this.process = process;
        this.marshaller = new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy());
        this.config = config;
        this.cqlSession = cqlSession;
        this.tableName = process.id().toUpperCase();

        if (config.createTables().orElse(Boolean.TRUE)) {
            createTable();
        }
    }

    @Override
    public Optional<? extends ProcessInstance> findById(String id, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return Optional.of(cachedInstances.get(resolvedId));
        }
        LOGGER.debug("findById() called for instance {}", resolvedId);

        Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName).column(CONTENT_FIELD)
                .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(resolvedId));

        ResultSet rs = cqlSession.execute(select.build());
        Row row = rs.one();
        if (row != null) {

            byte[] content = ByteUtils.getArray(row.getByteBuffer(CONTENT_FIELD));

            return Optional.of(mode == MUTABLE ? marshaller.unmarshallProcessInstance(content, process)
                    : marshaller.unmarshallReadOnlyProcessInstance(content, process));

        } else {
            return Optional.empty();
        }

    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int page, int size) {
        LOGGER.debug("values() called");
        Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName).column(CONTENT_FIELD);

        ResultSet rs = cqlSession.execute(select.build());

        return rs.all().stream().map(item -> {
            try {
                byte[] content = ByteUtils.getArray(item.getByteBuffer(CONTENT_FIELD));

                return mode == MUTABLE ? marshaller.unmarshallProcessInstance(content, process)
                        : marshaller.unmarshallReadOnlyProcessInstance(content, process);
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
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        LOGGER.debug("findByIdOrTag() called for values {}", values);

        List<Row> collected = new ArrayList<Row>();
        Set<String> distinct = new HashSet<String>();
        for (String value : values) {

            Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
                    .column(CONTENT_FIELD)
                    .whereColumn(TAGS_FIELD).contains(literal(value));

            ResultSet rs = cqlSession.execute(select.build());
            rs.all().stream().filter(r -> !distinct.contains(r.getString(INSTANCE_ID_FIELD))).forEach(r -> {
                distinct.add(r.getString(INSTANCE_ID_FIELD));
                collected.add(r);
            });
        }
        return collected.stream().map(item -> {
            try {
                byte[] content = ByteUtils.getArray(item.getByteBuffer(CONTENT_FIELD));

                return mode == MUTABLE ? marshaller.unmarshallProcessInstance(content, process)
                        : marshaller.unmarshallReadOnlyProcessInstance(content, process);
            } catch (AccessDeniedException e) {
                return null;
            }
        })
                .filter(pi -> pi != null)
                .collect(Collectors.toList());

    }

    @Override
    public Long size() {
        LOGGER.debug("size() called");
        Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName)
                .countAll();

        ResultSet rs = cqlSession.execute(select.build());
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
        Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName).column(INSTANCE_ID_FIELD)
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
            byte[] data = marshaller.marhsallProcessInstance(instance);
            if (data == null) {
                return;
            }

            Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
            tags.add(resolvedId);
            if (instance.businessKey() != null) {
                tags.add(instance.businessKey());
            }

            Insert insert = insertInto(config.keyspace().orElse("automatiko"), tableName)
                    .value(INSTANCE_ID_FIELD, literal(resolvedId))
                    .value(CONTENT_FIELD, bindMarker())
                    .value(TAGS_FIELD, bindMarker());

            try {
                cqlSession.execute(cqlSession.prepare(insert.build()).bind(ByteBuffer.wrap(data), tags));
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

        if (isActive(instance)) {
            LOGGER.debug("update() called for instance {}", resolvedId);
            byte[] data = marshaller.marhsallProcessInstance(instance);
            if (data == null) {
                return;
            }

            Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
            tags.add(resolvedId);
            if (instance.businessKey() != null) {
                tags.add(instance.businessKey());
            }

            SimpleStatement statement = QueryBuilder.update(config.keyspace().orElse("automatiko"), tableName)
                    .setColumn(CONTENT_FIELD, bindMarker())
                    .setColumn(TAGS_FIELD, bindMarker())
                    .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(resolvedId))
                    .build();

            cqlSession.execute(cqlSession.prepare(statement)
                    .bind(ByteBuffer.wrap(data), tags));

            disconnect(instance);

        }
        cachedInstances.remove(resolvedId);

    }

    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        LOGGER.debug("remove() called for instance {}", resolvedId);
        cachedInstances.remove(resolvedId);
        cachedInstances.remove(id);

        Delete deleteStatement = deleteFrom(config.keyspace().orElse("automatiko"), tableName).whereColumn(INSTANCE_ID_FIELD)
                .isEqualTo(literal(resolvedId)).ifExists();

        cqlSession.execute(deleteStatement.build());
    }

    protected void createTable() {
        CreateKeyspace createKs = createKeyspace(config.keyspace().orElse("automatiko")).ifNotExists().withSimpleStrategy(1);
        cqlSession.execute(createKs.build());

        CreateTable createTable = SchemaBuilder.createTable(config.keyspace().orElse("automatiko"), tableName)
                .ifNotExists()
                .withPartitionKey(INSTANCE_ID_FIELD, DataTypes.TEXT)
                .withColumn(CONTENT_FIELD, DataTypes.BLOB)
                .withColumn(TAGS_FIELD, DataTypes.setOf(DataTypes.TEXT));

        cqlSession.execute(createTable.build());

        CreateIndex index = SchemaBuilder.createIndex(tableName + "_IDX").ifNotExists()
                .onTable(config.keyspace().orElse("automatiko"), tableName).andColumn(TAGS_FIELD);
        cqlSession.execute(index.build());
    }

    protected void disconnect(ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                Select select = selectFrom(config.keyspace().orElse("automatiko"), tableName).column(CONTENT_FIELD)
                        .whereColumn(INSTANCE_ID_FIELD).isEqualTo(literal(instance.id()));

                ResultSet rs = cqlSession.execute(select.build());
                Row row = rs.one();
                if (row != null) {

                    byte[] reloaded = row.getByteBuffer(1).array();

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
    public ExportedProcessInstance exportInstance(String id, boolean abort) {
        Optional<? extends ProcessInstance> found = findById(id,
                abort ? ProcessInstanceReadMode.MUTABLE : ProcessInstanceReadMode.READ_ONLY);

        if (found.isPresent()) {
            ProcessInstance instance = found.get();
            ExportedProcessInstance exported = marshaller.exportProcessInstance(instance);

            if (abort) {
                instance.abort();
            }

            return exported;
        }
        throw new ProcessInstanceNotFoundException(id);
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
