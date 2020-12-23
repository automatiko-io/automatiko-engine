
package io.automatiko.engine.workflow.base.instance.impl.demo;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;

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
