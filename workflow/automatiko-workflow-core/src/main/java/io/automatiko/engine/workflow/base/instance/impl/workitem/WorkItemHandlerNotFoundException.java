package io.automatiko.engine.workflow.base.instance.impl.workitem;

public class WorkItemHandlerNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -7885981466813064455L;

	public WorkItemHandlerNotFoundException(String workItemName) {
		super("Could not find work item handler for " + workItemName);
	}
}
