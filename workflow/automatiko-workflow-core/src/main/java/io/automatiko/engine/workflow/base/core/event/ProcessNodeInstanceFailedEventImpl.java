package io.automatiko.engine.workflow.base.core.event;

import io.automatiko.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;

public class ProcessNodeInstanceFailedEventImpl extends ProcessEvent implements ProcessNodeInstanceFailedEvent {

    private static final long serialVersionUID = 510l;

    private NodeInstance nodeInstance;

    private String errorId;

    private String errorMessage;

    private Exception exception;

    public ProcessNodeInstanceFailedEventImpl(final ProcessInstance instance, NodeInstance nodeInstance,
            String errorId, String errorMessage, Exception exception, ProcessRuntime runtime) {
        super(instance, runtime);
        this.nodeInstance = nodeInstance;
        this.errorId = errorId;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    @Override
    public NodeInstance getNodeInstance() {
        return this.nodeInstance;
    }

    @Override
    public String getErrorId() {
        return errorId;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public String toString() {
        return "==>[ProcessNodeInstanceFailed(name=" + getProcessInstance().getProcessName() + "; id="
                + getProcessInstance().getId() + "; node instance=" + getNodeInstance().getId() + "; node name="
                + getNodeInstance().getNodeName() + "; exception=" + getException().getMessage() + ")]";
    }
}
