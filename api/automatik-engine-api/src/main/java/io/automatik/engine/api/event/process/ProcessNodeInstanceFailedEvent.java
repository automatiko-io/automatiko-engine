
package io.automatik.engine.api.event.process;

import io.automatik.engine.api.runtime.process.NodeInstance;

/**
 * An event when a work item has transition between life cycle phases
 */
public interface ProcessNodeInstanceFailedEvent extends ProcessEvent {

	/**
	 * Returns node instance that failed at execution
	 * 
	 * @return work item
	 */
	NodeInstance getNodeInstance();

	/**
	 * Returns the actual exception that was thrown at execution
	 * 
	 * @return transition
	 */
	Exception getException();

}
