
package io.automatik.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.List;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.context.AbstractContext;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;

/**
 * A for each node.
 * 
 * This node activates the contained subflow for each element of a collection.
 * The node continues if all activated the subflow has been completed for each
 * of the elements in the collection.
 * 
 */
public class ForEachNode extends CompositeContextNode {

    private static final long serialVersionUID = 510l;

    private String variableName;
    private String outputVariableName;
    private String collectionExpression;
    private String outputCollectionExpression;
    private String completionConditionExpression;
    private boolean waitForCompletion = true;

    private boolean sequential = false;

    public ForEachNode() {
        // Split
        ForEachSplitNode split = new ForEachSplitNode();
        split.setName("ForEachSplit");
        split.setMetaData("hidden", true);
        split.setMetaData("UniqueId", getMetaData("Uniqueid") + ":foreach:split");
        super.addNode(split);
        super.linkIncomingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
                new CompositeNode.NodeAndType(split,
                        io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE));
        // Composite node
        CompositeContextNode compositeNode = new CompositeContextNode();
        compositeNode.setName("ForEachComposite");
        compositeNode.setMetaData("hidden", true);
        compositeNode.setMetaData("UniqueId", getMetaData("Uniqueid") + ":foreach:composite");
        super.addNode(compositeNode);
        VariableScope variableScope = new VariableScope();
        compositeNode.addContext(variableScope);
        compositeNode.setDefaultContext(variableScope);
        // Join
        ForEachJoinNode join = new ForEachJoinNode();
        join.setName("ForEachJoin");
        join.setMetaData("hidden", true);
        join.setMetaData("UniqueId", getMetaData("Uniqueid") + ":foreach:join");
        super.addNode(join);
        super.linkOutgoingConnections(
                new CompositeNode.NodeAndType(join,
                        io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE),
                io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        new ConnectionImpl(super.getNode(1), io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
                getCompositeNode(), io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        new ConnectionImpl(getCompositeNode(), io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
                super.getNode(3), io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
    }

    public String getVariableName() {
        return variableName;
    }

    public DataType getVariableType() {
        if (variableName == null) {
            return null;
        }
        for (Variable variable : ((VariableScope) getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                .getVariables()) {
            if (variableName.equals(variable.getName())) {
                return variable.getType();
            }
        }
        return null;
    }

    public String getOutputVariableName() {
        return outputVariableName;
    }

    public DataType getOutputVariableType() {
        if (outputVariableName == null) {
            return null;
        }
        for (Variable variable : ((VariableScope) getCompositeNode().getDefaultContext(VariableScope.VARIABLE_SCOPE))
                .getVariables()) {
            if (outputVariableName.equals(variable.getName())) {
                return variable.getType();
            }
        }
        return null;
    }

    public CompositeContextNode getCompositeNode() {
        return (CompositeContextNode) super.getNode(2);
    }

    public ForEachSplitNode getForEachSplitNode() {
        return (ForEachSplitNode) super.getNode(1);
    }

    public ForEachJoinNode getForEachJoinNode() {
        return (ForEachJoinNode) super.getNode(3);
    }

    public void addNode(Node node) {
        getCompositeNode().addNode(node);
    }

    public void internalAddNode(Node node) {
        super.addNode(node);
    }

    public Node getNode(long id) {
        return getCompositeNode().getNode(id);
    }

    public Node internalGetNode(long id) {
        return super.getNode(id);
    }

    public Node[] getNodes() {
        return getCompositeNode().getNodes();
    }

    public Node[] internalGetNodes() {
        return super.getNodes();
    }

    public void removeNode(Node node) {
        getCompositeNode().removeNode(node);
    }

    protected void internalRemoveNode(Node node) {
        super.removeNode(node);
    }

    public void linkIncomingConnections(String inType, long inNodeId, String inNodeType) {
        getCompositeNode().linkIncomingConnections(inType, inNodeId, inNodeType);
    }

    public void linkOutgoingConnections(long outNodeId, String outNodeType, String outType) {
        getCompositeNode().linkOutgoingConnections(outNodeId, outNodeType, outType);
    }

    public CompositeNode.NodeAndType getLinkedIncomingNode(String inType) {
        return getCompositeNode().getLinkedIncomingNode(inType);
    }

    public CompositeNode.NodeAndType internalGetLinkedIncomingNode(String inType) {
        return super.getLinkedIncomingNode(inType);
    }

    public CompositeNode.NodeAndType getLinkedOutgoingNode(String inType) {
        return getCompositeNode().getLinkedOutgoingNode(inType);
    }

    public CompositeNode.NodeAndType internalGetLinkedOutgoingNode(String inType) {
        return super.getLinkedOutgoingNode(inType);
    }

    public void setVariable(String variableName, DataType type) {
        this.variableName = variableName;
        VariableScope variableScope = (VariableScope) getCompositeNode()
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        List<Variable> variables = variableScope.getVariables();
        if (variables == null) {
            variables = new ArrayList<Variable>();
            variableScope.setVariables(variables);
        }
        Variable variable = new Variable();
        variable.setId(variableName);
        variable.setName(variableName);
        variable.setType(type);
        variables.add(variable);
    }

    public void setOutputVariable(String variableName, DataType type) {
        this.outputVariableName = variableName;
        VariableScope variableScope = (VariableScope) getCompositeNode()
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        List<Variable> variables = variableScope.getVariables();
        if (variables == null) {
            variables = new ArrayList<Variable>();
            variableScope.setVariables(variables);
        }
        Variable variable = new Variable();
        variable.setId(variableName);
        variable.setName(variableName);
        variable.setType(type);
        variables.add(variable);

        Variable tmpvariable = new Variable();
        variable.setId("foreach_output");
        tmpvariable.setName("foreach_output");
        tmpvariable.setType(type);
        variables.add(tmpvariable);
    }

    public String getCollectionExpression() {
        return collectionExpression;
    }

    public void setCollectionExpression(String collectionExpression) {
        this.collectionExpression = collectionExpression;
    }

    public String getOutputCollectionExpression() {
        return outputCollectionExpression;
    }

    public void setOutputCollectionExpression(String collectionExpression) {
        this.outputCollectionExpression = collectionExpression;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    public static class ForEachSplitNode extends ExtendedNodeImpl {
        private static final long serialVersionUID = 510l;
    }

    public static class ForEachJoinNode extends ExtendedNodeImpl {
        private static final long serialVersionUID = 510l;
    }

    @Override
    public Context getContext(String contextType) {
        Context context = getCompositeNode().getDefaultContext(contextType);
        if (context != null) {
            return context;
        }
        return super.getContext(contextType);
    }

    @Override
    public void addContext(Context context) {
        getCompositeNode().addContext(context);
        ((AbstractContext) context).setContextContainer(getCompositeNode());
    }

    @Override
    public void setDefaultContext(Context context) {
        getCompositeNode().setDefaultContext(context);
        ((AbstractContext) context).setContextContainer(getCompositeNode());
    }

    @Override
    public List<Context> getContexts(String contextType) {
        List<Context> contexts = super.getContexts(contextType);
        if (contexts == null) {
            contexts = getCompositeNode().getContexts(contextType);
        }

        return contexts;
    }

    @Override
    public Context getContext(String contextType, long id) {
        Context ctx = super.getContext(contextType, id);
        if (ctx == null) {
            ctx = getCompositeNode().getContext(contextType, id);
        }

        return ctx;
    }

    public String getCompletionConditionExpression() {
        return completionConditionExpression;
    }

    public void setCompletionConditionExpression(String completionConditionExpression) {
        this.completionConditionExpression = completionConditionExpression;
    }

    public boolean isSequential() {
        return sequential;
    }

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }
}
