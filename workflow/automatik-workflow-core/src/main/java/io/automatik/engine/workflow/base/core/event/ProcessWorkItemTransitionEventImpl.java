package io.automatik.engine.workflow.base.core.event;

import io.automatik.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.workflow.workitem.Transition;

public class ProcessWorkItemTransitionEventImpl extends ProcessEvent implements ProcessWorkItemTransitionEvent {

	private static final long serialVersionUID = 510l;

	private WorkItem workItem;
	private Transition<?> transition;

	private boolean transitioned;

	public ProcessWorkItemTransitionEventImpl(final ProcessInstance instance, WorkItem workItem,
			Transition<?> transition, ProcessRuntime runtime, boolean transitioned) {
		super(instance, runtime);
		this.workItem = workItem;
		this.transition = transition;
		this.transitioned = transitioned;
	}

	public String toString() {
		return "==>[WorkItemTransition(id=" + getWorkItem().getId() + " phase=" + getTransition().phase() + ")]";
	}

	@Override
	public WorkItem getWorkItem() {
		return workItem;
	}

	@Override
	public Transition<?> getTransition() {
		return transition;
	}

	@Override
	public boolean isTransitioned() {
		return transitioned;
	}

}
