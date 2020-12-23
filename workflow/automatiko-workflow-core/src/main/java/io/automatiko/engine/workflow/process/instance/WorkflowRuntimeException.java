
package io.automatiko.engine.workflow.process.instance;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;

/**
 * This exception provides the context information of the error in execution of
 * the flow. <br/>
 * It would be helpful to located the error instead of confusing stack trace
 */
public class WorkflowRuntimeException extends RuntimeException {

    /** Generated serial version uid */
    private static final long serialVersionUID = 8210449548783940188L;

    private String processInstanceId;
    private String processId;
    private String nodeInstanceId;
    private long nodeId;
    private String nodeName;
    private String deploymentId;

    private Map<String, Object> variables;

    public WorkflowRuntimeException(NodeInstance nodeInstance, ProcessInstance processInstance, String message) {
        super(message);
        initialize(nodeInstance, processInstance);
    }

    public WorkflowRuntimeException(NodeInstance nodeInstance, ProcessInstance processInstance, String message,
            Throwable e) {
        super(message, e);
        initialize(nodeInstance, processInstance);
    }

    public WorkflowRuntimeException(NodeInstance nodeInstance, ProcessInstance processInstance, Exception e) {
        super(e);
        initialize(nodeInstance, processInstance);
    }

    private void initialize(NodeInstance nodeInstance, ProcessInstance processInstance) {
        this.processInstanceId = processInstance.getId();
        this.processId = processInstance.getProcessId();
        if (nodeInstance != null) {
            this.nodeInstanceId = nodeInstance.getId();
            this.nodeId = nodeInstance.getNodeId();
            if (((ProcessInstanceImpl) processInstance).getProcessRuntime() != null) {
                this.nodeName = nodeInstance.getNodeName();
            }
        }

        VariableScopeInstance variableScope = (VariableScopeInstance) ((io.automatiko.engine.workflow.base.instance.ProcessInstance) processInstance)
                .getContextInstance(VariableScope.VARIABLE_SCOPE);
        // set input parameters
        if (variableScope != null) {
            this.variables = variableScope.getVariables();
        } else {
            this.variables = new HashMap<String, Object>(0);
        }
    }

    /**
     * @return the processInstanceId
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * @param processInstanceId the processInstanceId to set
     */
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * @return the processId
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * @param processId the processId to set
     */
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * @return the nodeInstanceId
     */
    public String getNodeInstanceId() {
        return nodeInstanceId;
    }

    /**
     * @param nodeInstanceId the nodeInstanceId to set
     */
    public void setNodeInstanceId(String nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }

    /**
     * @return the nodeId
     */
    public long getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeId the nodeId to set
     */
    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName the nodeName to set
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("[{0}:{4} - {1}:{2}] -- {3}", getProcessId(),
                (getNodeName() == null ? "?" : getNodeName()), (getNodeId() == 0 ? "?" : getNodeId()),
                (getCause() == null ? "WorkflowRuntimeException" : getCause().getMessage()), getProcessInstanceId());
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

}
