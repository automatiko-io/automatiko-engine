
package io.automatik.engine.workflow.process.instance;

import java.util.Collection;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.NodeContainer;

/**
 * 
 */
public interface NodeInstanceContainer extends io.automatik.engine.api.runtime.process.NodeInstanceContainer {

	Collection<NodeInstance> getNodeInstances(boolean recursive);

	NodeInstance getFirstNodeInstance(long nodeId);

	NodeInstance getNodeInstance(Node node);

	void addNodeInstance(NodeInstance nodeInstance);

	void removeNodeInstance(NodeInstance nodeInstance);

	NodeContainer getNodeContainer();

	void nodeInstanceCompleted(NodeInstance nodeInstance, String outType);

	int getState();

	void setState(int state);

	int getLevelForNode(String uniqueID);

	void setCurrentLevel(int level);

	int getCurrentLevel();

	NodeInstance getNodeInstance(String nodeInstanceId, boolean recursive);

}
