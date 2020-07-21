
package io.automatik.engine.api.definition.process;

import java.io.Serializable;

/**
 * A NodeContainer contains a set of Nodes There are different types of
 * NodeContainers and NodeContainers may be nested.
 */
public interface NodeContainer extends Serializable {

	/**
	 * The Nodes of this NodeContainer.
	 *
	 * @return the nodes
	 */
	Node[] getNodes();

	/**
	 * The node in this NodeContainer with the given id.
	 *
	 * @return the node with the given id
	 */
	Node getNode(long id);

}
