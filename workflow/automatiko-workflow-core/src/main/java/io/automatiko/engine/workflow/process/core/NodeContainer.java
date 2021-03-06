
package io.automatiko.engine.workflow.process.core;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.Context;

public interface NodeContainer extends io.automatiko.engine.api.definition.process.NodeContainer {

    /**
     * Method for adding a node to this node container. Note that the node will get
     * an id unique for this node container.
     * 
     * @param node the node to be added
     * @throws IllegalArgumentException if <code>node</code> is null
     */
    void addNode(Node node);

    /**
     * Method for removing a node from this node container
     * 
     * @param node the node to be removed
     * @throws IllegalArgumentException if <code>node</code> is null or unknown
     */
    void removeNode(Node node);

    Context resolveContext(String contextId, Object param);

    Node internalGetNode(long id);

    default List<Node> getAutoStartNodes() {

        List<Node> nodes = Arrays.stream(getNodes())
                .filter(n -> n.getIncomingConnections().isEmpty()
                        && "true".equalsIgnoreCase((String) n.getMetaData().get("customAutoStart")))
                .collect(Collectors.toList());

        return nodes;
    }

}
