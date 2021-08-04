
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class CompositeContextNodeFactory extends ExecutableNodeContainerFactory {

    public static final String METHOD_VARIABLE = "variable";
    public static final String METHOD_LINK_INCOMING_CONNECTIONS = "linkIncomingConnections";
    public static final String METHOD_LINK_OUTGOING_CONNECTIONS = "linkOutgoingConnections";
    public static final String METHOD_AUTO_COMPLETE = "autoComplete";

    private ExecutableNodeContainerFactory nodeContainerFactory;
    private NodeContainer nodeContainer;
    private long linkedIncomingNodeId = -1;
    private long linkedOutgoingNodeId = -1;

    public CompositeContextNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
            long id) {
        this.nodeContainerFactory = nodeContainerFactory;
        this.nodeContainer = nodeContainer;
        CompositeContextNode node = createNode();
        node.setId(id);
        setNodeContainer(node);
    }

    protected CompositeContextNode createNode() {
        return new CompositeContextNode();
    }

    protected CompositeContextNode getCompositeNode() {
        return (CompositeContextNode) getNodeContainer();
    }

    public CompositeContextNodeFactory name(String name) {
        getCompositeNode().setName(name);
        return this;
    }

    public CompositeContextNodeFactory variable(String name, DataType type) {
        return variable(name, type, null);
    }

    public CompositeContextNodeFactory variable(String name, DataType type, Object value) {
        return variable(name, type, value, null, null);
    }

    public CompositeContextNodeFactory variable(String name, DataType type, String metaDataName, Object metaDataValue) {
        return variable(name, type, null, metaDataName, metaDataValue);
    }

    public CompositeContextNodeFactory variable(String name, DataType type, Object value, String metaDataName,
            Object metaDataValue) {
        Variable variable = new Variable();
        variable.setName(name);
        variable.setType(type);
        variable.setValue(value);
        if (metaDataName != null && metaDataValue != null) {
            variable.setMetaData(metaDataName, metaDataValue);
        }
        VariableScope variableScope = (VariableScope) getCompositeNode()
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        if (variableScope == null) {
            variableScope = new VariableScope();
            getCompositeNode().addContext(variableScope);
            getCompositeNode().setDefaultContext(variableScope);
        }
        variableScope.getVariables().add(variable);
        return this;
    }

    public CompositeContextNodeFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
        ExceptionScope exceptionScope = (ExceptionScope) getCompositeNode()
                .getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
        if (exceptionScope == null) {
            exceptionScope = new ExceptionScope();
            getCompositeNode().addContext(exceptionScope);
            getCompositeNode().setDefaultContext(exceptionScope);
        }
        for (String error : exception.split(",")) {
            exceptionScope.setExceptionHandler(error, exceptionHandler);
        }
        return this;
    }

    public CompositeContextNodeFactory exceptionHandler(String exception, String dialect, String action) {
        ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
        exceptionHandler.setAction(new ConsequenceAction(dialect, action));
        return exceptionHandler(exception, exceptionHandler);
    }

    public CompositeContextNodeFactory autoComplete(boolean autoComplete) {
        getCompositeNode().setAutoComplete(autoComplete);
        return this;
    }

    public CompositeContextNodeFactory linkIncomingConnections(long nodeId) {
        this.linkedIncomingNodeId = nodeId;
        return this;
    }

    public CompositeContextNodeFactory linkOutgoingConnections(long nodeId) {
        this.linkedOutgoingNodeId = nodeId;
        return this;
    }

    public CompositeContextNodeFactory metaData(String name, Object value) {
        getCompositeNode().setMetaData(name, value);
        return this;
    }

    public ExecutableNodeContainerFactory done() {
        if (linkedIncomingNodeId != -1) {
            getCompositeNode().linkIncomingConnections(Node.CONNECTION_DEFAULT_TYPE, linkedIncomingNodeId,
                    Node.CONNECTION_DEFAULT_TYPE);
        }
        if (linkedOutgoingNodeId != -1) {
            getCompositeNode().linkOutgoingConnections(linkedOutgoingNodeId, Node.CONNECTION_DEFAULT_TYPE,
                    Node.CONNECTION_DEFAULT_TYPE);
        }
        nodeContainer.addNode(getCompositeNode());
        return nodeContainerFactory;
    }
}
