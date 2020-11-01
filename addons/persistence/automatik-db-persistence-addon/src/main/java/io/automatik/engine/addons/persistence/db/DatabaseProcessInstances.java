package io.automatik.engine.addons.persistence.db;

import static io.automatik.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.addons.persistence.db.model.ProcessInstanceEntity;
import io.automatik.engine.api.auth.AccessDeniedException;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.workflow.AbstractProcess;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatik.engine.workflow.marshalling.ProcessInstanceMarshaller;
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
    public Optional<ProcessInstance<ProcessInstanceEntity>> findById(String id, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);

        Optional<ProcessInstanceEntity> found = (Optional<ProcessInstanceEntity>) JpaOperations.findByIdOptional(type,
                resolvedId);

        if (found.isEmpty()) {
            return Optional.empty();
        }
        ProcessInstanceEntity entity = found.get();
        return Optional.of(unmarshallInstance(mode, entity));
    }

    @Override
    public Collection<? extends ProcessInstance<ProcessInstanceEntity>> findByIdOrTag(ProcessInstanceReadMode mode,
            String... values) {
        return JpaOperations.stream(type, "id in (?1) or (?1) in elements(tags) ", Arrays.asList(values))
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
    public Collection<ProcessInstance<ProcessInstanceEntity>> values(ProcessInstanceReadMode mode) {
        return JpaOperations.streamAll(type)
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
    public Integer size() {
        return (int) JpaOperations.count(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        Optional<ProcessInstanceEntity> found = (Optional<ProcessInstanceEntity>) JpaOperations.findByIdOptional(type,
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
        JpaOperations.persist(entity);
        // then delete the root one
        JpaOperations.deleteById(type, resolveId(id, instance));
    }

    protected void store(String id, ProcessInstance<ProcessInstanceEntity> instance) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance)) {
            ProcessInstanceEntity entity = instance.variables();
            byte[] data = marshaller.marhsallProcessInstance(instance);

            entity.content = data;
            entity.id = resolvedId;
            entity.name = instance.description();
            entity.businessKey = instance.businessKey();
            entity.processId = instance.process().id();
            entity.processName = instance.process().name();
            entity.processVersion = instance.process().version();
            entity.startDate = instance.startDate();
            entity.state = instance.status();

            entity.tags = new HashSet<>(instance.tags().values());

            JpaOperations.persist(entity);
            disconnect(instance);
        }
    }

    protected void disconnect(ProcessInstance<ProcessInstanceEntity> instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                ProcessInstanceEntity entity = (ProcessInstanceEntity) JpaOperations.findById(type,
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
            pi = ((AbstractProcess<ProcessInstanceEntity>) process).createInstance(wpi, entity);

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

}
