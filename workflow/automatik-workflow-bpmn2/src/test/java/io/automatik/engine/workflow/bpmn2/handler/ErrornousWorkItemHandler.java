
package io.automatik.engine.workflow.bpmn2.handler;

import io.automatik.engine.api.runtime.process.ProcessWorkItemHandlerException;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.runtime.process.ProcessWorkItemHandlerException.HandlingStrategy;

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
