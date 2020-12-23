
package io.automatiko.engine.api.event.process;

import io.automatiko.engine.api.runtime.process.NodeInstance;

/**
 * An event related to the execution of a node instance within a process
 * instance.
 */
public interface ProcessNodeEvent extends ProcessEvent {

	/**
	 * The node instance this event is related to.
	 *
	 * @return the node instance
	 */
	NodeInstance getNodeInstance();

}
