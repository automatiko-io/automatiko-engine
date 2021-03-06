package io.automatiko.engine.workflow.base.instance.impl.actions;

import java.io.Serializable;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.instance.impl.Action;

public class ProcessInstanceCompensationAction implements Action, Serializable {

	private static final long serialVersionUID = 1L;

	private final String activityRef;

	public ProcessInstanceCompensationAction(String activityRef) {
		this.activityRef = activityRef;
	}

	public void execute(ProcessContext context) throws Exception {
		context.getProcessInstance().signalEvent("Compensation", activityRef);
	}

}
