
package io.automatiko.engine.workflow.base.instance.impl.demo;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;

/**
 * 
 */
public class DoNothingWorkItemHandler implements WorkItemHandler {

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

}
