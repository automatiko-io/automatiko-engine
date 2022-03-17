package io.automatiko.engine.workflow.base.core.event;

import io.automatiko.engine.api.event.process.ProcessNodeInitializedEvent;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;

public class ProcessNodeInitializedEventImpl extends ProcessEvent implements ProcessNodeInitializedEvent {

    private static final long serialVersionUID = 510l;

    private NodeInstance nodeInstance;

    public ProcessNodeInitializedEventImpl(final NodeInstance nodeInstance, ProcessRuntime runtime) {
        super(nodeInstance.getProcessInstance(), runtime);
        this.nodeInstance = nodeInstance;
    }

    public NodeInstance getNodeInstance() {
        return nodeInstance;
    }

    public String toString() {
        return "==>[ProcessNodeInitialized(nodeId=" + nodeInstance.getNodeId() + "; id=" + nodeInstance.getId()
                + "; nodeName=" + getNodeInstance().getNodeName() + "; processName="
                + getProcessInstance().getProcessName() + "; processId=" + getProcessInstance().getProcessId() + ")]";
    }
}
