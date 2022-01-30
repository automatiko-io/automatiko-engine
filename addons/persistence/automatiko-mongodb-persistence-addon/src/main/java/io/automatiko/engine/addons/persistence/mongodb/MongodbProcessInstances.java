package io.automatiko.engine.addons.persistence.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.config.MongodbPersistenceConfig;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MongodbProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongodbProcessInstances.class);

    private static final String INSTANCE_ID_FIELD = "instanceId";
    private static final String CONTENT_FIELD = "content";
    private static final String TAGS_FIELD = "tags";
    private static final String VERSION_FIELD = "versionTrack";
    private static final String STATUS_FIELD = "piStatus";
    private static final String VARIABLES_FIELD = "variables";

    private MongoClient mongoClient;
    private MongodbPersistenceConfig config;

    private JacksonObjectMarshallingStrategy marshallingStrategy;
    private final Process<? extends Model> process;
    private final ProcessInstanceMarshaller marshaller;
    private final StoredDataCodec codec;

    private String tableName;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public MongodbProcessInstances(Process<? extends Model> process, MongoClient mongoClient,
            MongodbPersistenceConfig config, StoredDataCodec codec) {
        this.process = process;
        this.marshallingStrategy = new JacksonObjectMarshallingStrategy();
        this.marshaller = new ProcessInstanceMarshaller(marshallingStrategy);
        this.config = config;
        this.mongoClient = mongoClient;
        this.tableName = process.id();
        this.codec = codec;

        // mark the marshaller that it should not serialize variables
        this.marshaller.addToEnvironment("_ignore_vars_", true);

        collection().createIndex(Indexes.compoundIndex(Indexes.ascending(INSTANCE_ID_FIELD), Indexes.ascending(STATUS_FIELD)),
                new IndexOptions().unique(true));
        collection().createIndex(Indexes.ascending(TAGS_FIELD));
    }

    @Override
    public Optional findById(String id, int status, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);

        Document found = collection().find(and(eq(INSTANCE_ID_FIELD, resolvedId), eq(STATUS_FIELD, status)))
                .projection(Projections
                        .fields(Projections.include(INSTANCE_ID_FIELD, CONTENT_FIELD, VERSION_FIELD, VARIABLES_FIELD)))
                .first();

        if (found == null) {
            return Optional.empty();
        }

        return Optional.of(unmarshallInstance(mode, found));

    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size) {
        Collection found = new HashSet<>();
        collection().find(eq(STATUS_FIELD, status))
                .projection(Projections
                        .fields(Projections.include(INSTANCE_ID_FIELD, CONTENT_FIELD, VERSION_FIELD, VARIABLES_FIELD)))
                .skip(calculatePage(page, size))
                .limit(size)
                .forEach(item -> found.add(unmarshallInstance(mode, item)));
        return found;
    }

    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values) {
        Collection found = new HashSet<>();
        collection().find(and(in(TAGS_FIELD, values), eq(STATUS_FIELD, status)))
                .projection(Projections
                        .fields(Projections.include(INSTANCE_ID_FIELD, CONTENT_FIELD, VERSION_FIELD, VARIABLES_FIELD)))
                .forEach(item -> found.add(unmarshallInstance(mode, item)));
        return found;
    }

    @Override
    public Collection<String> locateByIdOrTag(int status, String... values) {
        Collection<String> found = new HashSet<>();
        collection().find(and(in(TAGS_FIELD, values), eq(STATUS_FIELD, status)))
                .projection(Projections.fields(Projections.include(INSTANCE_ID_FIELD)))
                .forEach(item -> found.add(item.getString(INSTANCE_ID_FIELD)));
        return found;
    }

    @Override
    public Long size() {
        return collection().countDocuments();
    }

    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return true;
        }
        LOGGER.debug("exists() called for instance {}", resolvedId);
        Document found = collection().find(eq(INSTANCE_ID_FIELD, resolvedId))
                .projection(Projections.fields(Projections.include(INSTANCE_ID_FIELD))).first();

        return found != null;
    }

    @Override
    public void create(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        try {
            if (isActive(instance)) {

                byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));

                if (data == null) {
                    return;
                }
                Model entity = (Model) instance.variables();

                String variablesJson = marshallingStrategy.mapper().writeValueAsString(entity);

                Document variables = Document.parse(variablesJson);
                removeTransientVariables(variables, instance);

                Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
                tags.add(resolvedId);
                if (instance.businessKey() != null) {
                    tags.add(instance.businessKey());
                }

                Document item = new Document(INSTANCE_ID_FIELD, resolvedId)
                        .append(CONTENT_FIELD, data)
                        .append(STATUS_FIELD, instance.status())
                        .append(TAGS_FIELD, tags)
                        .append(VERSION_FIELD, ((AbstractProcessInstance<?>) instance).getVersionTracker())
                        .append(VARIABLES_FIELD, variables);

                try {
                    collection().insertOne(item);
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void update(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        try {
            if (isActive(instance)) {

                byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));

                if (data == null) {
                    return;
                }
                Model entity = (Model) instance.variables();

                String variablesJson = marshallingStrategy.mapper().writeValueAsString(entity);

                Document variables = Document.parse(variablesJson);
                removeTransientVariables(variables, instance);

                Collection<String> tags = new LinkedHashSet<>(instance.tags().values());
                tags.add(resolvedId);
                if (instance.businessKey() != null) {
                    tags.add(instance.businessKey());
                }

                Document item = new Document(INSTANCE_ID_FIELD, resolvedId)
                        .append(CONTENT_FIELD, data)
                        .append(STATUS_FIELD, instance.status())
                        .append(TAGS_FIELD, tags)
                        .append(VERSION_FIELD, ((AbstractProcessInstance<?>) instance).getVersionTracker())
                        .append(VARIABLES_FIELD, variables);

                try {
                    Document replaced = collection().findOneAndReplace(and(eq(INSTANCE_ID_FIELD, resolvedId),
                            eq(VERSION_FIELD, ((AbstractProcessInstance<?>) instance).getVersionTracker())), item);

                    if (replaced == null) {
                        throw new ConflictingVersionException("Process instance with id '" + instance.id()
                                + "' has older version than the stored one");
                    }
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);

        collection().findOneAndDelete(eq(INSTANCE_ID_FIELD, resolvedId));
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

    /*
     * Helper methods
     */

    protected MongoCollection<Document> collection() {
        MongoDatabase database = mongoClient.getDatabase(config.database().orElse("automatiko"));
        return database.getCollection(tableName);
    }

    protected ProcessInstance unmarshallInstance(ProcessInstanceReadMode mode, Document entity) {
        try {
            ProcessInstance pi;
            if (mode == MUTABLE) {
                WorkflowProcessInstance wpi = marshaller
                        .unmarshallWorkflowProcessInstance(codec.decode(entity.get(CONTENT_FIELD, Binary.class).getData()),
                                process);
                String variablesJson = entity.get(VARIABLES_FIELD, Document.class).toJson();
                Model model = process.createModel();

                Map<String, Object> loaded = marshallingStrategy.mapper().readValue(variablesJson, model.getClass()).toMap();
                model.fromMap(loaded);
                loaded.forEach((k, v) -> {
                    if (v != null) {
                        v.toString();
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                                .getContextInstance(VariableScope.VARIABLE_SCOPE);
                        variableScopeInstance.internalSetVariable(k, v);
                    }
                });

                pi = ((AbstractProcess) process).createInstance(wpi, model, entity.getLong(VERSION_FIELD));

            } else {
                WorkflowProcessInstance wpi = marshaller
                        .unmarshallWorkflowProcessInstance(codec.decode(entity.get(CONTENT_FIELD, Binary.class).getData()),
                                process);
                String variablesJson = entity.get(VARIABLES_FIELD, Document.class).toJson();
                Model model = process.createModel();

                Map<String, Object> loaded = marshallingStrategy.mapper().readValue(variablesJson, model.getClass()).toMap();
                model.fromMap(loaded);
                loaded.forEach((k, v) -> {
                    if (v != null) {
                        v.toString();
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                                .getContextInstance(VariableScope.VARIABLE_SCOPE);
                        variableScopeInstance.internalSetVariable(k, v);
                    }
                });
                pi = ((AbstractProcess) process).createReadOnlyInstance(wpi, model);
            }

            return pi;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void disconnect(ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                String resolvedId = instance.id();

                Document returnedItem = collection().find(eq(INSTANCE_ID_FIELD, resolvedId))
                        .projection(Projections
                                .fields(Projections.include(INSTANCE_ID_FIELD, CONTENT_FIELD, VERSION_FIELD, VARIABLES_FIELD)))
                        .first();

                if (returnedItem != null) {
                    byte[] reloaded = returnedItem.get(CONTENT_FIELD, Binary.class).getData();

                    WorkflowProcessInstance wpi = marshaller
                            .unmarshallWorkflowProcessInstance(codec.decode(reloaded), process);
                    String variablesJson = returnedItem.get(VARIABLES_FIELD, Document.class).toJson();
                    Model model = process.createModel();

                    Map<String, Object> loaded = marshallingStrategy.mapper().readValue(variablesJson, model.getClass())
                            .toMap();
                    model.fromMap(loaded);
                    loaded.forEach((k, v) -> {
                        if (v != null) {
                            v.toString();
                            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
                            variableScopeInstance.internalSetVariable(k, v);
                        }
                    });
                    return wpi;
                } else {
                    return null;
                }
            } catch (IOException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }

        });
    }

    protected void removeTransientVariables(Document variables, ProcessInstance<?> instance) {

        VariableScope variableScope = (VariableScope) ((WorkflowProcess) ((AbstractProcess<?>) instance.process())
                .process()).getDefaultContext(VariableScope.VARIABLE_SCOPE);

        for (Variable var : variableScope.getVariables()) {
            if (var.hasTag(Variable.TRANSIENT_TAG)) {
                variables.remove(var.getName());
            }
        }
    }
}