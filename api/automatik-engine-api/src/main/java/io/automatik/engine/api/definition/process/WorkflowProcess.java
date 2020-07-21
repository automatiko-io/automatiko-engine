
package io.automatik.engine.api.definition.process;

import java.util.List;

/**
 * A WorkflowProcess is a type of Process that uses a flow chart (as a
 * collection of Nodes and Connections) to model the business logic.
 */
public interface WorkflowProcess extends Process, NodeContainer {

	String PUBLIC_VISIBILITY = "Public";
	String PRIVATE_VISIBILITY = "Private";
	String NONE_VISIBILITY = "None";

	String getVisibility();

	List<Node> getNodesRecursively();

}
