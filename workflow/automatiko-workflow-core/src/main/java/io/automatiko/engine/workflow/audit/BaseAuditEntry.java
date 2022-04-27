package io.automatiko.engine.workflow.audit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class BaseAuditEntry implements AuditEntry {

    private static ObjectMapper MAPPER = new ObjectMapper().registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    private Type type;

    private LinkedHashMap<String, Object> items = new LinkedHashMap<>();

    public BaseAuditEntry(Type type) {
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    @Override
    public String toRawString() {
        return this.items.toString();
    }

    @Override
    public String toJsonString() {
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to create JSON represantation of audit entry", e);
        }
    }

    @Override
    public AuditEntry add(String name, Object value) {
        if (name != null && value != null) {
            this.items.put(name, value);
        }
        return this;
    }

    @Override
    public Map<String, Object> items() {
        return this.items;
    }

    public static AuditEntry create(Type type) {
        BaseAuditEntry entry = new BaseAuditEntry(type);
        entry.add(TIMESTAMP, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        entry.add(IDENTITY, IdentityProvider.get().getName());
        return entry;
    }

    public static AuditEntry persitenceRead(io.automatiko.engine.api.workflow.ProcessInstance<?> processInstance) {
        AuditEntry entry = create(Type.WORKFLOW_PERSISTENCE_READ)
                .add(WORKFLOW_DEFINITION_ID, processInstance.process().id())
                .add(WORKFLOW_INSTANCE_ID, processInstance.id())
                .add(WORKFLOW_ROOT_DEFINITION_ID, processInstance.rootProcessId())
                .add(WORKFLOW_DEFINITION_NAME, processInstance.process().name())
                .add(WORKFLOW_DEFINITION_VERSION, processInstance.process().version())
                .add(WORKFLOW_ROOT_INSTANCE_ID, processInstance.rootProcessInstanceId())
                .add(WORKFLOW_PARENT_INSTANCE_ID, processInstance.parentProcessInstanceId())
                .add(WORKFLOW_INSTANCE_STATE, processInstance.status())
                .add(BUSINESS_KEY, processInstance.businessKey())
                .add(DESCRIPTION, processInstance.description())
                .add(TAGS, processInstance.tags().values());
        return entry;
    }

    public static AuditEntry persitenceWrite(io.automatiko.engine.api.workflow.ProcessInstance<?> processInstance) {
        AuditEntry entry = create(Type.WORKFLOW_PERSISTENCE_WRITE)
                .add(WORKFLOW_DEFINITION_ID, processInstance.process().id())
                .add(WORKFLOW_INSTANCE_ID, processInstance.id())
                .add(WORKFLOW_ROOT_DEFINITION_ID, processInstance.rootProcessId())
                .add(WORKFLOW_DEFINITION_NAME, processInstance.process().name())
                .add(WORKFLOW_DEFINITION_VERSION, processInstance.process().version())
                .add(WORKFLOW_ROOT_INSTANCE_ID, processInstance.rootProcessInstanceId())
                .add(WORKFLOW_PARENT_INSTANCE_ID, processInstance.parentProcessInstanceId())
                .add(WORKFLOW_INSTANCE_STATE, processInstance.status())
                .add(BUSINESS_KEY, processInstance.businessKey())
                .add(DESCRIPTION, processInstance.description())
                .add(TAGS, processInstance.tags().values());

        return entry;
    }

    public static AuditEntry messaging(ProcessInstance processInstance, String connector, String message, Object payload) {
        AuditEntry entry = create(Type.MESSAGING)
                .add(WORKFLOW_DEFINITION_ID, processInstance.getProcessId())
                .add(WORKFLOW_INSTANCE_ID, processInstance.getId())
                .add(WORKFLOW_ROOT_DEFINITION_ID, processInstance.getRootProcessId())
                .add(WORKFLOW_DEFINITION_NAME, processInstance.getProcessName())
                .add(WORKFLOW_DEFINITION_VERSION, processInstance.getProcess().getVersion())
                .add(WORKFLOW_ROOT_INSTANCE_ID, processInstance.getRootProcessInstanceId())
                .add(WORKFLOW_PARENT_INSTANCE_ID, processInstance.getParentProcessInstanceId())
                .add(WORKFLOW_INSTANCE_STATE, processInstance.getState())
                .add(BUSINESS_KEY, processInstance.getCorrelationKey())
                .add(CONNECTOR, connector)
                .add(MESSAGE_NAME, message)
                .add(PAYLOAD, payload)
                .add(TAGS, ((WorkflowProcessInstanceImpl) processInstance).getTags().stream().map(t -> t.getValue())
                        .collect(Collectors.toList()));

        return entry;
    }

    public static AuditEntry timer() {
        return create(Type.TIMER);
    }

    public static AuditEntry timer(ProcessJobDescription description) {
        AuditEntry entry = create(Type.TIMER)
                .add(JOB_ID, description.id())
                .add(EXPIRES_AT,
                        description.expirationTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .add(TIMER_EXPRESSION, description.expirationTime().expression())
                .add(WORKFLOW_DEFINITION_ID, description.processId())
                .add(WORKFLOW_DEFINITION_VERSION, description.processVersion());

        return entry;
    }

    public static AuditEntry timer(ProcessInstanceJobDescription description) {
        AuditEntry entry = create(Type.TIMER)
                .add(JOB_ID, description.id())
                .add(EXPIRES_AT,
                        description.expirationTime().get() != null
                                ? description.expirationTime().get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                : null)
                .add(TIMER_EXPRESSION, description.expirationTime().expression())
                .add(WORKFLOW_DEFINITION_ID, description.processId())
                .add(WORKFLOW_DEFINITION_VERSION, description.processVersion())
                .add(WORKFLOW_INSTANCE_ID, description.processInstanceId())
                .add(TRIGGER_TYPE, description.triggerType());

        return entry;
    }

    public static AuditEntry workflow(ProcessInstance processInstance) {
        AuditEntry entry = create(Type.WORKFLOW)
                .add(WORKFLOW_DEFINITION_ID, processInstance.getProcessId())
                .add(WORKFLOW_INSTANCE_ID, processInstance.getId())
                .add(WORKFLOW_ROOT_DEFINITION_ID, processInstance.getRootProcessId())
                .add(WORKFLOW_DEFINITION_NAME, processInstance.getProcessName())
                .add(WORKFLOW_DEFINITION_VERSION, processInstance.getProcess().getVersion())
                .add(WORKFLOW_ROOT_INSTANCE_ID, processInstance.getRootProcessInstanceId())
                .add(WORKFLOW_PARENT_INSTANCE_ID, processInstance.getParentProcessInstanceId())
                .add(WORKFLOW_INSTANCE_STATE, processInstance.getState())
                .add(BUSINESS_KEY, processInstance.getCorrelationKey())
                .add(DESCRIPTION, ((WorkflowProcessInstanceImpl) processInstance).getDescription())
                .add(VARIABLES, processInstance.getPublicVariables())
                .add(TAGS, ((WorkflowProcessInstanceImpl) processInstance).getTags().stream().map(t -> t.getValue())
                        .collect(Collectors.toList()));

        if (((WorkflowProcessInstanceImpl) processInstance).getProcessRuntime() != null) {
            String uowIdentifier = ((WorkflowProcessInstanceImpl) processInstance).getProcessRuntime().getUnitOfWorkManager()
                    .currentUnitOfWork().identifier();
            entry.add(TRANSACTION_ID, uowIdentifier);
        }
        return entry;
    }

    public static AuditEntry workflow(ProcessInstance processInstance, NodeInstance nodeInstance) {
        AuditEntry entry = workflow(processInstance);
        ((BaseAuditEntry) entry).type = Type.WORKFLOW_NODE;
        if (nodeInstance != null) {
            entry.add(NODEINSTANCE_ID, nodeInstance.getId())
                    .add(NODE_DEFINITION_ID, nodeInstance.getNodeDefinitionId())
                    .add(NODE_NAME, nodeInstance.getNodeName())
                    .add(NODE_INSTANCE_STATE, nodeInstance.getNodeInstanceState().name())
                    .add(NODE_TYPE, nodeInstance.getClass().getSimpleName());
        }
        return entry;
    }

    public static AuditEntry workflow(ProcessInstance processInstance, NodeInstance nodeInstance, String variable) {
        AuditEntry entry = workflow(processInstance);
        ((BaseAuditEntry) entry).type = Type.WORKFLOW_VARIABLE;
        if (nodeInstance != null) {
            entry.add(NODEINSTANCE_ID, nodeInstance.getId())
                    .add(NODE_DEFINITION_ID, nodeInstance.getNodeDefinitionId())
                    .add(NODE_NAME, nodeInstance.getNodeName())
                    .add(NODE_INSTANCE_STATE, nodeInstance.getNodeInstanceState().name())
                    .add(NODE_TYPE, nodeInstance.getClass().getSimpleName());
        }
        entry.add(VARIABLE_NAME, variable);
        return entry;
    }
}
