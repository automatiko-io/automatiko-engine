package io.automatiko.engine.addons.persistence.db;

import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.OptimisticLockException;

import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.persistence.db.model.ProcessInstanceEntity;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public class DatabaseProcessInstances implements MutableProcessInstances<ProcessInstanceEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseProcessInstances.class);

    private final Process<? extends ProcessInstanceEntity> process;
    private ProcessInstanceMarshaller marshaller;

    private Class<? extends ProcessInstanceEntity> type;

    public DatabaseProcessInstances(Process<? extends ProcessInstanceEntity> process) {
        this.process = process;
        this.marshaller = new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy());

        this.type = process.createModel().getClass();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ProcessInstance<ProcessInstanceEntity>> findById(String id, int status, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);

        Optional<ProcessInstanceEntity> found = (Optional<ProcessInstanceEntity>) JpaOperations.INSTANCE.findByIdOptional(type,
                resolvedId);

        if (found.isEmpty()) {
            return Optional.empty();
        }
        ProcessInstanceEntity entity = found.get();
        if (entity.state == status) {
            return Optional.of(unmarshallInstance(mode, entity));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Collection<? extends ProcessInstance<ProcessInstanceEntity>> findByIdOrTag(ProcessInstanceReadMode mode,
            int status, String... values) {
        return JpaOperations.INSTANCE
                .stream(type, "state = ?1 and (id in (?2) or (?2) in elements(tags)) ", status, Arrays.asList(values))
                .map(e -> {
                    try {
                        return unmarshallInstance(mode, ((ProcessInstanceEntity) e));
                    } catch (AccessDeniedException ex) {
                        return null;
                    }
                })
                .filter(pi -> pi != null)
                .collect(Collectors.toSet());

    }

    @Override
    public Collection<ProcessInstance<ProcessInstanceEntity>> values(ProcessInstanceReadMode mode, int status, int page,
            int size) {
        return JpaOperations.INSTANCE.find(type, "state = ?1 ", status).page(calculatePage(page, size), size)
                .stream()
                .map(e -> {
                    try {
                        return unmarshallInstance(mode, ((ProcessInstanceEntity) e));
                    } catch (AccessDeniedException ex) {
                        return null;
                    }
                })
                .filter(pi -> pi != null)
                .collect(Collectors.toList());
    }

    @Override
    public Long size() {
        return JpaOperations.INSTANCE.count(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        Optional<ProcessInstanceEntity> found = (Optional<ProcessInstanceEntity>) JpaOperations.INSTANCE.findByIdOptional(type,
                resolvedId);
        return found.isPresent();
    }

    @Override
    public void create(String id, ProcessInstance<ProcessInstanceEntity> instance) {
        store(id, instance);
    }

    @Override
    public void update(String id, ProcessInstance<ProcessInstanceEntity> instance) {
        store(id, instance);
    }

    @Override
    public void remove(String id, ProcessInstance<ProcessInstanceEntity> instance) {
        ProcessInstanceEntity entity = instance.variables();
        // run persist to make sure entities of the root are stored
        JpaOperations.INSTANCE.persist(entity);
        // then delete the root one
        JpaOperations.INSTANCE.deleteById(type, resolveId(id, instance));
    }

    protected void store(String id, ProcessInstance<ProcessInstanceEntity> instance) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance)) {
            ProcessInstanceEntity entity = instance.variables();
            byte[] data = marshaller.marhsallProcessInstance(instance);

            if (data == null) {
                return;
            }

            entity.content = data;
            entity.entityId = resolvedId;
            entity.name = instance.description();
            entity.businessKey = instance.businessKey();
            entity.processId = instance.process().id();
            entity.processName = instance.process().name();
            entity.processVersion = instance.process().version();
            entity.startDate = instance.startDate();
            entity.state = instance.status();

            entity.tags = new HashSet<>(instance.tags().values());
            try {
                JpaOperations.INSTANCE.persist(entity);
            } catch (OptimisticLockException | StaleObjectStateException e) {
                throw new ConflictingVersionException("Process instance with id '" + instance.id()
                        + "' has older version than tha stored one");
            } finally {
                disconnect(instance);
            }
        }
    }

    protected void disconnect(ProcessInstance<ProcessInstanceEntity> instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                ProcessInstanceEntity entity = (ProcessInstanceEntity) JpaOperations.INSTANCE.findById(type,
                        resolveId(instance.id(), instance));
                byte[] reloaded = entity.content;

                WorkflowProcessInstance wpi = marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
                entity.toMap().forEach((k, v) -> {
                    if (v != null) {
                        v.toString();
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                                .getContextInstance(VariableScope.VARIABLE_SCOPE);
                        variableScopeInstance.internalSetVariable(k, v);
                    }
                });
                return wpi;
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }

        });
    }

    @SuppressWarnings("unchecked")
    protected ProcessInstance<ProcessInstanceEntity> unmarshallInstance(ProcessInstanceReadMode mode,
            ProcessInstanceEntity entity) {
        ProcessInstance<ProcessInstanceEntity> pi;
        if (mode == MUTABLE) {
            WorkflowProcessInstance wpi = marshaller.unmarshallWorkflowProcessInstance(entity.content, process);
            entity.toMap().forEach((k, v) -> {
                if (v != null) {
                    v.toString();
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                            .getContextInstance(VariableScope.VARIABLE_SCOPE);
                    variableScopeInstance.internalSetVariable(k, v);
                }
            });
            pi = ((AbstractProcess<ProcessInstanceEntity>) process).createInstance(wpi, entity, entity.version);

        } else {
            WorkflowProcessInstance wpi = marshaller.unmarshallWorkflowProcessInstance(entity.content, process);
            entity.toMap().forEach((k, v) -> {
                if (v != null) {
                    v.toString();
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ProcessInstanceImpl) wpi)
                            .getContextInstance(VariableScope.VARIABLE_SCOPE);
                    variableScopeInstance.internalSetVariable(k, v);
                }
            });
            pi = ((AbstractProcess<ProcessInstanceEntity>) process).createReadOnlyInstance(wpi, entity);
        }

        return pi;
    }

    @Override
    public ExportedProcessInstance exportInstance(ProcessInstance<?> instance, boolean abort) {

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
