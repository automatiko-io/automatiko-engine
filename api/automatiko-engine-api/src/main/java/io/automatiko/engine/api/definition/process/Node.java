
package io.automatiko.engine.api.definition.process;

import java.util.List;
import java.util.Map;

/**
 * A Node represents an activity in the process flow chart. Many different
 * predefined nodes are supported out-of-the-box.
 */
public interface Node {

    /**
     * The id of the node. This is unique within its NodeContainer.
     * 
     * @return the id of the node
     */
    long getId();

    /**
     * The name of the node
     * 
     * @return the name of the node
     */
    String getName();

    /**
     * The incoming connections for this Node. A Node could have multiple
     * entry-points. This map contains the list of incoming connections for each
     * entry-point.
     *
     * @return the incoming connections
     */
    Map<String, List<Connection>> getIncomingConnections();

    /**
     * The outgoing connections for this Node. A Node could have multiple
     * exit-points. This map contains the list of outgoing connections for each
     * exit-point.
     *
     * @return the outgoing connections
     */
    Map<String, List<Connection>> getOutgoingConnections();

    /**
     * The incoming connections for this Node for the given entry-point.
     *
     * @return the incoming connections for the given entry point
     */
    List<Connection> getIncomingConnections(String type);

    /**
     * The outgoing connections for this Node for the given exit-point.
     *
     * @return the outgoing connections for the given exit point
     */
    List<Connection> getOutgoingConnections(String type);

    /**
     * The NodeContainer this Node lives in.
     *
     * @return the parent NodeContainer
     */
    NodeContainer getParentContainer();

    /**
     * Meta data associated with this Node.
     */
    Map<String, Object> getMetaData();

    /**
     * Verifies if given event type matches any of the listeners of this node
     * 
     * @param type type of the incoming event
     * @return returns true if the event matches otherwise false
     */
    boolean hasMatchingEventListner(String type);

}
