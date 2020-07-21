
package io.automatik.engine.api.workflow.workitem;

import java.util.Collection;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemManager;

/**
 * Complete life cycle that can be applied to work items. It defines set of
 * phases and allow to get access to each of them by id.
 *
 * @param <T> defines the type of data managed through this life cycle
 */
public interface LifeCycle<T> {

	/**
	 * Returns phase by its id if exists.
	 * 
	 * @param phaseId phase id to be used for look up
	 * @return life cycle phase if exists otherwise null
	 */
	LifeCyclePhase phaseById(String phaseId);

	/**
	 * Returns all phases associated with this life cycle
	 * 
	 * @return list of phases
	 */
	Collection<LifeCyclePhase> phases();

	/**
	 * Perform actual transition to the target phase defined via given transition
	 * 
	 * @param workItem   work item that is being transitioned
	 * @param manager    work item manager for given work item
	 * @param transition actual transition
	 * @return returns work item data after the transition
	 */
	T transitionTo(WorkItem workItem, WorkItemManager manager, Transition<T> transition);

	/**
	 * Returns current data set for given work item
	 * 
	 * @param workItem work item to get the data for
	 * @return current data set
	 */
	T data(WorkItem workItem);
}
