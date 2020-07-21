
package io.automatik.engine.api.event.process;

import io.automatik.engine.api.runtime.process.NodeInstance;

/**
 * An event when a SLA has been violated.
 */
public interface SLAViolatedEvent extends ProcessEvent {

	/**
	 * The node instance this event is related to.
	 *
	 * @return the node instance
	 */
	NodeInstance getNodeInstance();

}
