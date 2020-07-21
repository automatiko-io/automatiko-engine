
package io.automatik.engine.workflow.bpmn2.objects;

import java.util.ArrayList;
import java.util.List;

import io.automatik.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.workflow.workitem.Transition;
import io.automatik.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemImpl;
import io.automatik.engine.workflow.base.instance.impl.workitem.Active;
import io.automatik.engine.workflow.base.instance.impl.workitem.Complete;
import io.automatik.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatik.engine.workflow.base.instance.impl.workitem.WorkItemImpl;

public class TestWorkItemHandler implements WorkItemHandler {

	private String name;

	public TestWorkItemHandler() {
	}

	public TestWorkItemHandler(String name) {
		this.name = name;
	}

	private List<WorkItem> workItems = new ArrayList<WorkItem>();

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		workItems.add(workItem);

		if (workItem instanceof HumanTaskWorkItem) {
			HumanTaskWorkItemImpl humanTaskWorkItem = (HumanTaskWorkItemImpl) workItem;

			humanTaskWorkItem.setPhaseId(Active.ID);
			humanTaskWorkItem.setPhaseStatus(Active.STATUS);
		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public WorkItem getWorkItem() {
		if (workItems.size() == 0) {
			return null;
		}
		if (workItems.size() == 1) {
			WorkItem result = workItems.get(0);
			this.workItems.clear();
			return result;
		} else {
			throw new IllegalArgumentException("More than one work item active");
		}
	}

	public List<WorkItem> getWorkItems() {
		List<WorkItem> result = new ArrayList<WorkItem>(workItems);
		workItems.clear();
		return result;
	}

	@Override
	public void transitionToPhase(WorkItem workItem, WorkItemManager manager, Transition<?> transition) {

		if (transition.phase().equals(Complete.ID)) {
			((DefaultWorkItemManager) manager).internalCompleteWorkItem((WorkItemImpl) workItem);
		}
	}

	@Override
	public String getName() {
		return name;
	}

}
