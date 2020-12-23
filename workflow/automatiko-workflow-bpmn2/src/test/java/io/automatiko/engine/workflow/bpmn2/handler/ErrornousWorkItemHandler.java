
package io.automatiko.engine.workflow.bpmn2.handler;

import io.automatiko.engine.api.runtime.process.ProcessWorkItemHandlerException;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.runtime.process.ProcessWorkItemHandlerException.HandlingStrategy;

public class ErrornousWorkItemHandler implements WorkItemHandler {

	private String processId;
	private HandlingStrategy strategy;

	private WorkItem workItem;

	public ErrornousWorkItemHandler(String processId, HandlingStrategy strategy) {
		super();
		this.processId = processId;
		this.strategy = strategy;
	}

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.workItem = workItem;
		if (processId != null && strategy != null) {

			if (workItem.getParameter("isCheckedCheckbox") != null) {
				manager.completeWorkItem(workItem.getId(), workItem.getParameters());
			} else {

				throw new ProcessWorkItemHandlerException(processId, strategy, new RuntimeException("On purpose"));
			}
		}

		manager.completeWorkItem(workItem.getId(), null);
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.workItem = workItem;

	}

	public WorkItem getWorkItem() {
		return workItem;
	}
}
