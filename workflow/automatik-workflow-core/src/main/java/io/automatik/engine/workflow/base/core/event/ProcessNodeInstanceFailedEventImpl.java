package io.automatik.engine.workflow.base.core.event;

import io.automatik.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class ProcessNodeInstanceFailedEventImpl extends ProcessEvent implements ProcessNodeInstanceFailedEvent {

	private static final long serialVersionUID = 510l;

	private NodeInstance nodeInstance;
	private Exception exception;

	public ProcessNodeInstanceFailedEventImpl(final ProcessInstance instance, NodeInstance nodeInstance,
			Exception exception, ProcessRuntime runtime) {
		super(instance, runtime);
		this.nodeInstance = nodeInstance;
		this.exception = exception;
	}

	@Override
	public NodeInstance getNodeInstance() {
		return this.nodeInstance;
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
