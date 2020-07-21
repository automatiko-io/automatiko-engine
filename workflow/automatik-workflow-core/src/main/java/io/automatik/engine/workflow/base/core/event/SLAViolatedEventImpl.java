package io.automatik.engine.workflow.base.core.event;

import io.automatik.engine.api.event.process.SLAViolatedEvent;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class SLAViolatedEventImpl extends ProcessEvent implements SLAViolatedEvent {

	private static final long serialVersionUID = 510l;
	private NodeInstance nodeInstance;

	public SLAViolatedEventImpl(final ProcessInstance instance, ProcessRuntime runtime) {
		super(instance, runtime);
	}

	public SLAViolatedEventImpl(final ProcessInstance instance, NodeInstance nodeInstance, ProcessRuntime runtime) {
		super(instance, runtime);
		this.nodeInstance = nodeInstance;
	}

	public String toString() {
		return "==>[SLAViolatedEvent(name=" + getProcessInstance().getProcessName() + "; id="
				+ getProcessInstance().getProcessId() + ")]";
	}

	@Override
	public NodeInstance getNodeInstance() {
		return nodeInstance;
	}

}
