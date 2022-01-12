package io.automatiko.engine.workflow.base.core.context;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;

public class ProcessContext implements io.automatiko.engine.api.runtime.process.ProcessContext {

    private static Logger logger = LoggerFactory.getLogger(ProcessContext.class);

    private ProcessRuntime runtime;
    private ProcessInstance processInstance;
    private NodeInstance nodeInstance;

    public ProcessContext(ProcessRuntime runtime) {
        this.runtime = runtime;
    }

    public ProcessInstance getProcessInstance() {
        if (processInstance != null) {
            return processInstance;
        }
        if (nodeInstance != null) {
            return nodeInstance.getProcessInstance();
        }
        return null;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public NodeInstance getNodeInstance() {
        return nodeInstance;
    }

    public ProcessContext setNodeInstance(NodeInstance nodeInstance) {
        this.nodeInstance = nodeInstance;
        return this;
    }

    public Object getVariable(String variableName) {
        if (nodeInstance != null) {
            return nodeInstance.getVariable(variableName);
        } else {
            return ((WorkflowProcessInstance) getProcessInstance()).getVariable(variableName);
        }
    }

    public void setVariable(String variableName, Object value) {
        if (nodeInstance != null) {
            nodeInstance.setVariable(variableName, value);
        } else {
            ((WorkflowProcessInstance) getProcessInstance()).setVariable(variableName, value);
        }
    }

    public ProcessRuntime getProcessRuntime() {
        return runtime;
    }

    @Override
    public Map<String, Object> getVariables() {
        if (processInstance == null) {
            return Collections.emptyMap();
        }
        return processInstance.getVariables();
    }

}
