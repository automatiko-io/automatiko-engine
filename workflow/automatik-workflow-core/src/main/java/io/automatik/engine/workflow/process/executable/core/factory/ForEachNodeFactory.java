
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class ForEachNodeFactory extends CompositeContextNodeFactory {

    public static final String METHOD_COLLECTION_EXPRESSION = "collectionExpression";
    public static final String METHOD_OUTPUT_COLLECTION_EXPRESSION = "outputCollectionExpression";
    public static final String METHOD_OUTPUT_VARIABLE = "outputVariable";

    public ForEachNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
            long id) {
        super(nodeContainerFactory, nodeContainer, id);
    }

    protected ForEachNode getForEachNode() {
        return (ForEachNode) getNodeContainer();
    }

    @Override
    protected CompositeContextNode createNode() {
        return new ForEachNode();
    }

    @Override
    public ForEachNodeFactory name(String name) {
        super.name(name);
        return this;
    }

    public ForEachNodeFactory collectionExpression(String collectionExpression) {
        getForEachNode().setCollectionExpression(collectionExpression);
        return this;
    }

    @Override
    public ForEachNodeFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
        super.exceptionHandler(exception, exceptionHandler);
        return this;
    }

    @Override
    public ForEachNodeFactory exceptionHandler(String exception, String dialect, String action) {
        super.exceptionHandler(exception, dialect, action);
        return this;
    }

    @Override
    public ForEachNodeFactory autoComplete(boolean autoComplete) {
        super.autoComplete(autoComplete);
        return this;
    }

    @Override
    public ForEachNodeFactory linkIncomingConnections(long nodeId) {
        super.linkIncomingConnections(nodeId);
        return this;
    }

    @Override
    public ForEachNodeFactory linkOutgoingConnections(long nodeId) {
        super.linkOutgoingConnections(nodeId);
        return this;
    }

    @Override
    public ForEachNodeFactory variable(String name, DataType type) {
        getForEachNode().setVariable(name, type);
        return this;
    }

    public ForEachNodeFactory outputCollectionExpression(String collectionExpression) {
        getForEachNode().setOutputCollectionExpression(collectionExpression);
        return this;
    }

    public ForEachNodeFactory outputVariable(String variableName, DataType dataType) {
        getForEachNode().setOutputVariable(variableName, dataType);
        return this;
    }

    public ForEachNodeFactory waitForCompletion(boolean waitForCompletion) {
        getForEachNode().setWaitForCompletion(waitForCompletion);
        return this;
    }

    public ForEachNodeFactory sequential(boolean sequential) {
        getForEachNode().setSequential(sequential);
        return this;
    }
}
