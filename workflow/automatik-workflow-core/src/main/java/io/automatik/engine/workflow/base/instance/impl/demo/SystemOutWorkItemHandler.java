
package io.automatik.engine.workflow.base.instance.impl.demo;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;

/**
 * 
 */
public class SystemOutWorkItemHandler implements WorkItemHandler {

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		System.out.println("Executing work item " + workItem);
		manager.completeWorkItem(workItem.getId(), null);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		System.out.println("Aborting work item " + workItem);
		manager.abortWorkItem(workItem.getId());
	}

}
