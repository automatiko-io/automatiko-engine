
package io.automatik.engine.api.event.process;

import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.workflow.workitem.Transition;

/**
 * An event when a work item has transition between life cycle phases
 */
public interface ProcessWorkItemTransitionEvent extends ProcessEvent {

	/**
	 * Returns work item being transitioned
	 * 
	 * @return work item
	 */
	WorkItem getWorkItem();

	/**
	 * Returns transition that is applied to the work item
	 * 
	 * @return transition
	 */
	Transition<?> getTransition();

	/**
	 * Indicated is the transition has already been done.
	 * 
	 * @return true if transition has already been done, otherwise false
	 */
	boolean isTransitioned();
}
