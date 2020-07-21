
package io.automatik.engine.workflow.base.instance.impl.humantask;

import java.util.Map;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.workflow.workitem.LifeCycle;
import io.automatik.engine.api.workflow.workitem.Transition;
import io.automatik.engine.workflow.base.instance.impl.workitem.Abort;
import io.automatik.engine.workflow.base.instance.impl.workitem.Active;

/**
 * Work item handler to be used with human tasks (work items). It uses
 * <code>BaseHumanTaskLifeCycle</code> by default but allows to plug in another
 * life cycle implementation.
 *
 */
public class HumanTaskWorkItemHandler implements WorkItemHandler {

	private LifeCycle<Map<String, Object>> lifeCycle;

	public HumanTaskWorkItemHandler() {
		this(new BaseHumanTaskLifeCycle());
	}

	public HumanTaskWorkItemHandler(LifeCycle<Map<String, Object>> lifeCycle) {
		this.lifeCycle = lifeCycle;
	}

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		lifeCycle.transitionTo(workItem, manager, new HumanTaskTransition(Active.ID));
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		lifeCycle.transitionTo(workItem, manager, new HumanTaskTransition(Abort.ID));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void transitionToPhase(WorkItem workItem, WorkItemManager manager, Transition<?> transition) {

		lifeCycle.transitionTo(workItem, manager, (Transition<Map<String, Object>>) transition);
	}

}
