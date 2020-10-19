
package io.automatik.engine.workflow.process.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextResolver;
import io.automatik.engine.workflow.process.core.Constraint;
import io.automatik.engine.workflow.process.core.ExpressionCondition;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.CompositeNode;

/**
 * Default implementation of a node.
 */
public abstract class NodeImpl implements Node, ContextResolver {

    private static final long serialVersionUID = 510l;

    public static final String ACTIVATION_EVENTS = "ActivationEvents";
    public static final String EXIT_EVENTS = "ExitEvents";

    private long id;
    private static final AtomicLong uniqueIdGen = new AtomicLong(0);

    private String name;
    private Map<String, List<Connection>> incomingConnections;
    private Map<String, List<Connection>> outgoingConnections;
    private NodeContainer parentContainer;
    private Map<String, Context> contexts = new HashMap<>();
    private Map<String, Object> metaData = new HashMap<>();

    private transient Optional<ExpressionCondition> activationCheck;
    private transient Optional<ExpressionCondition> completionCheck;

    protected Map<ConnectionRef, Constraint> constraints = new HashMap<ConnectionRef, Constraint>();

    public NodeImpl() {
        this.id = -1;
        this.incomingConnections = new HashMap<>();
        this.outgoingConnections = new HashMap<>();

        this.activationCheck = Optional.empty();
        this.completionCheck = Optional.empty();
    }

    public long getId() {
        return this.id;
    }

    public String getUniqueId() {
        String result = id + "";
        NodeContainer nodeContainer = getParentContainer();
        while (nodeContainer instanceof CompositeNode) {
            CompositeNode composite = (CompositeNode) nodeContainer;
            result = composite.getId() + ":" + result;
            nodeContainer = composite.getParentContainer();
        }
        return result;
    }

    public void setId(final long id) {
        this.id = id;
        String uniqueId = (String) getMetaData("UniqueId");
        if (uniqueId == null) {
            setMetaData("UniqueId", "_jbpm-unique-" + uniqueIdGen.getAndIncrement());
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Map<String, List<Connection>> getIncomingConnections() {
        // TODO: users can still modify the lists inside this Map
        return Collections.unmodifiableMap(this.incomingConnections);
    }

    public Map<String, List<Connection>> getOutgoingConnections() {
        // TODO: users can still modify the lists inside this Map
        return Collections.unmodifiableMap(this.outgoingConnections);
    }

    public void addIncomingConnection(final String type, final Connection connection) {
        validateAddIncomingConnection(type, connection);
        List<Connection> connections = this.incomingConnections.get(type);
        if (connections == null) {
            connections = new ArrayList<Connection>();
            this.incomingConnections.put(type, connections);
        }
        connections.add(connection);
    }

    public void validateAddIncomingConnection(final String type, final Connection connection) {
        if (type == null) {
            throw new IllegalArgumentException("Connection type cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
    }

    public List<Connection> getIncomingConnections(String type) {
        List<Connection> result = incomingConnections.get(type);
        if (result == null) {
            return new ArrayList<Connection>();
        }
        return result;
    }

    public void addOutgoingConnection(final String type, final Connection connection) {
        validateAddOutgoingConnection(type, connection);
        List<Connection> connections = this.outgoingConnections.get(type);
        if (connections == null) {
            connections = new ArrayList<Connection>();
            this.outgoingConnections.put(type, connections);
        }
        connections.add(connection);
    }

    public void validateAddOutgoingConnection(final String type, final Connection connection) {
        if (type == null) {
            throw new IllegalArgumentException("Connection type cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
    }

    public List<Connection> getOutgoingConnections(String type) {
        List<Connection> result = outgoingConnections.get(type);
        if (result == null) {
            return new ArrayList<Connection>();
        }
        return result;
    }

    public void removeIncomingConnection(final String type, final Connection connection) {
        validateRemoveIncomingConnection(type, connection);
        this.incomingConnections.get(type).remove(connection);
    }

    public void clearIncomingConnection() {
        this.incomingConnections.clear();
    }

    public void clearOutgoingConnection() {
        this.outgoingConnections.clear();
    }

    public void validateRemoveIncomingConnection(final String type, final Connection connection) {
        if (type == null) {
            throw new IllegalArgumentException("Connection type cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection is null");
        }
        if (!incomingConnections.get(type).contains(connection)) {
            throw new IllegalArgumentException(
                    "Given connection <" + connection + "> is not part of the incoming connections");
        }
    }

    public void removeOutgoingConnection(final String type, final Connection connection) {
        validateRemoveOutgoingConnection(type, connection);
        this.outgoingConnections.get(type).remove(connection);
    }

    public void validateRemoveOutgoingConnection(final String type, final Connection connection) {
        if (type == null) {
            throw new IllegalArgumentException("Connection type cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection is null");
        }
        if (!this.outgoingConnections.get(type).contains(connection)) {
            throw new IllegalArgumentException(
                    "Given connection <" + connection + "> is not part of the outgoing connections");
        }
    }

    /**
     * Helper method for nodes that have at most one default incoming connection
     */
    public Connection getFrom() {
        final List<Connection> list = getIncomingConnections(
                io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        if (list.size() == 0) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        if ("true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            return list.get(0);
        } else {
            throw new IllegalArgumentException(
                    "Trying to retrieve the from connection but multiple connections are present");
        }
    }

    /**
     * Helper method for nodes that have at most one default outgoing connection
     */
    public Connection getTo() {
        final List<Connection> list = getOutgoingConnections(
                io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        if (list.size() == 0) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        if ("true".equals(System.getProperty("jbpm.enable.multi.con"))) {
            return list.get(0);
        } else {
            throw new IllegalArgumentException(
                    "Trying to retrieve the to connection but multiple connections are present");
        }
    }

    /**
     * Helper method for nodes that have multiple default incoming connections
     */
    public List<Connection> getDefaultIncomingConnections() {
        return getIncomingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
    }

    /**
     * Helper method for nodes that have multiple default outgoing connections
     */
    public List<Connection> getDefaultOutgoingConnections() {
        return getOutgoingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
    }

    public NodeContainer getParentContainer() {
        return parentContainer;
    }

    public void setParentContainer(NodeContainer nodeContainer) {
        this.parentContainer = nodeContainer;
    }

    public void setContext(String contextId, Context context) {
        this.contexts.put(contextId, context);
    }

    public Context getContext(String contextId) {
        return this.contexts.get(contextId);
    }

    public Context resolveContext(String contextId, Object param) {
        Context context = getContext(contextId);
        if (context != null) {
            context = context.resolveContext(param);
            if (context != null) {
                return context;
            }
        }
        return ((io.automatik.engine.workflow.process.core.NodeContainer) parentContainer).resolveContext(contextId,
                param);
    }

    public void setMetaData(String name, Object value) {
        this.metaData.put(name, value);
    }

    public Object getMetaData(String name) {
        return this.metaData.get(name);
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    public Constraint getConstraint(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }

        ConnectionRef ref = new ConnectionRef((String) connection.getMetaData().get("UniqueId"),
                connection.getTo().getId(), connection.getToType());
        return this.constraints.get(ref);

    }

    public Constraint internalGetConstraint(final ConnectionRef ref) {
        return this.constraints.get(ref);
    }

    public void setConstraint(final Connection connection, final Constraint constraint) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (!getDefaultOutgoingConnections().contains(connection)) {
            throw new IllegalArgumentException("connection is unknown:" + connection);
        }
        addConstraint(new ConnectionRef((String) connection.getMetaData().get("UniqueId"), connection.getTo().getId(),
                connection.getToType()), constraint);

    }

    public void addConstraint(ConnectionRef connectionRef, Constraint constraint) {
        if (connectionRef == null) {
            throw new IllegalArgumentException(
                    "A " + this.getName() + " node only accepts constraints linked to a connection");
        }
        this.constraints.put(connectionRef, constraint);
    }

    public Map<ConnectionRef, Constraint> getConstraints() {
        return Collections.unmodifiableMap(this.constraints);
    }

    @Override
    public Optional<ExpressionCondition> getActivationCheck() {
        if (activationCheck == null) {
            activationCheck = Optional.empty();
        }
        return activationCheck;
    }

    @Override
    public void setActivationCheck(Optional<ExpressionCondition> activationCheck) {
        this.activationCheck = activationCheck;
    }

    @Override
    public Optional<ExpressionCondition> getCompletionCheck() {
        if (completionCheck == null) {
            completionCheck = Optional.empty();
        }
        return completionCheck;
    }

    @Override
    public void setCompletionCheck(Optional<ExpressionCondition> completionCheck) {
        this.completionCheck = completionCheck;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean hasMatchingEventListner(String type) {
        if (type.equals(getName())) {
            return true;
        }
        boolean result = false;
        List<String> activationEvents = (List<String>) getMetaData(ACTIVATION_EVENTS);
        if (activationEvents != null && activationEvents.contains(type)) {
            result = true;
        }
        return result;
    }

}
