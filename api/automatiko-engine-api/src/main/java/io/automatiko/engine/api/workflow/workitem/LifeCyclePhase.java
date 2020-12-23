
package io.automatiko.engine.api.workflow.workitem;

import io.automatiko.engine.api.runtime.process.WorkItem;

/**
 * Definition of the life cycle phase that work item can be connected to.
 *
 */
public interface LifeCyclePhase {

	/**
	 * Returns unique id of this life cycle phase
	 * 
	 * @return phase id
	 */
	String id();

	/**
	 * Returns status associated with this life cycle phase
	 * 
	 * @return phase status
	 */
	String status();

	/**
	 * Returns if given state is the terminating phase (final state) for given work
	 * item
	 * 
	 * @return true if this is final phase otherwise false
	 */
	boolean isTerminating();

	/**
	 * Returns if given life cycle phase can be transitioned to this phase
	 * 
	 * @param phase phase to be transitioned from
	 * @return true if phase can be transitioned from to this one otherwise false
	 */
	boolean canTransition(LifeCyclePhase phase);

	/**
	 * Optional extra work to be applied on work item upon transition to this phase
	 * 
	 * @param workitem   work item that is being transitioned
	 * @param transition actual transition
	 */
	default void apply(WorkItem workitem, Transition<?> transition) {

	}
}
