
package io.automatiko.engine.workflow.process.test;

import java.util.Deque;
import java.util.LinkedList;

import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;

/**
 * 
 */
public class TestWorkItemHandler implements WorkItemHandler {

	public Deque<WorkItem> workItems = new LinkedList<WorkItem>();

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.workItems.add(workItem);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.workItems.add(workItem);
	}

	public Deque<WorkItem> getWorkItems() {
		return this.workItems;
	}
}
