
package io.automatik.engine.workflow.bpmn2.objects;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;

public class ExceptionOnPurposeHandler implements WorkItemHandler {

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		throw new RuntimeException("Thrown on purpose");
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}
}
