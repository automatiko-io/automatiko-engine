package io.automatiko.engine.api.audit;

import java.util.Map;

public interface AuditEntry {

    public static final String TRANSACTION_ID = "transactionId";
    public static final String TIMESTAMP = "timestamp";
    public static final String IDENTITY = "identity";
    public static final String MESSAGE = "message";

    public static final String WORKFLOW_DEFINITION_ID = "workflowDefinitionId";
    public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    public static final String WORKFLOW_ROOT_DEFINITION_ID = "workflowRootDefinitionId";
    public static final String WORKFLOW_DEFINITION_NAME = "workflowDefinitionName";
    public static final String WORKFLOW_DEFINITION_VERSION = "workflowDefinitionVersion";
    public static final String WORKFLOW_ROOT_INSTANCE_ID = "workflowRootInstanceId";
    public static final String WORKFLOW_PARENT_INSTANCE_ID = "workflowParentInstanceId";
    public static final String WORKFLOW_INSTANCE_STATE = "workflowInstanceState";
    public static final String BUSINESS_KEY = "businessKey";
    public static final String DESCRIPTION = "description";
    public static final String TAGS = "tags";
    public static final String VARIABLES = "variables";

    public static final String CONNECTOR = "connector";
    public static final String MESSAGE_NAME = "messageName";
    public static final String PAYLOAD = "payload";

    public static final String JOB_ID = "jobId";
    public static final String EXPIRES_AT = "expiresAt";
    public static final String TIMER_EXPRESSION = "timerExpression";
    public static final String TRIGGER_TYPE = "triggerType";

    public static final String NODEINSTANCE_ID = "nodeInstanceId";
    public static final String NODE_DEFINITION_ID = "nodeDefinitionId";
    public static final String NODE_NAME = "nodeName";
    public static final String NODE_INSTANCE_STATE = "nodeInstanceState";
    public static final String NODE_TYPE = "nodeType";
    public static final String VARIABLE_NAME = "variableName";

    public enum Type {
        MESSAGING,
        TIMER,
        WORKFLOW,
        WORKFLOW_NODE,
        WORKFLOW_VARIABLE,
        WORKFLOW_PERSISTENCE_READ,
        WORKFLOW_PERSISTENCE_WRITE
    }

    /**
     * Returns type of the audit entry
     * 
     * @return type of the entry
     */
    Type type();

    /**
     * Returns plain text representation of this audit entry
     * 
     * @return plain text representation of this entry
     */
    String toRawString();

    /**
     * Returns JSON text representation of this audit entry
     * 
     * @return JSON text representation of this entry
     */
    String toJsonString();

    /**
     * Adds new item to the audit entry
     * 
     * @param entry data item for audit entry
     * @return current audit entry
     */
    AuditEntry add(String name, Object value);

    /**
     * Returns all items associated with the audit entry
     * 
     * @return non null map of data items
     */
    Map<String, Object> items();
}
