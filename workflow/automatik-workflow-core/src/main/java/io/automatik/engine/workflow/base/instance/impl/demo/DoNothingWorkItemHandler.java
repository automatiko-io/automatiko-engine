
package io.automatik.engine.workflow.base.instance.impl.demo;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;

/**
 * 
 */
public class DoNothingWorkItemHandler implements WorkItemHandler {

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

}
