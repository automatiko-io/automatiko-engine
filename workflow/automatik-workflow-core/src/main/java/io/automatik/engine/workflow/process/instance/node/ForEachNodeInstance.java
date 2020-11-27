
package io.automatik.engine.workflow.process.instance.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.SimpleValueResolver;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.core.ExpressionCondition;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode.ForEachJoinNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode.ForEachSplitNode;
import io.automatik.engine.workflow.process.core.node.Transformation;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of a for each node.
 */
public class ForEachNodeInstance extends CompositeContextNodeInstance {

    private static final long serialVersionUID = 510L;

    private static final String TEMP_OUTPUT_VAR = "foreach_output";

    private int sequentialCounter = 0;

    public ForEachNode getForEachNode() {
        return (ForEachNode) getNode();
    }

    @Override
    public NodeInstance getNodeInstance(final Node node) {
        if (node instanceof ForEachSplitNode) {
            ForEachSplitNodeInstance nodeInstance = new ForEachSplitNodeInstance();
            nodeInstance.setNodeId(node.getId());
            nodeInstance.setNodeInstanceContainer(this);
            nodeInstance.setProcessInstance(getProcessInstance());
            String uniqueID = (String) node.getMetaData().get("UniqueId");
            if (uniqueID == null) {
                uniqueID = node.getId() + "";
            }
            int level = this.getLevelForNode(uniqueID);
            nodeInstance.setLevel(level);
            return nodeInstance;
        } else if (node instanceof ForEachJoinNode) {
            ForEachJoinNodeInstance nodeInstance = (ForEachJoinNodeInstance) getFirstNodeInstance(node.getId());
            if (nodeInstance == null) {
                nodeInstance = new ForEachJoinNodeInstance();
                nodeInstance.setNodeId(node.getId());
                nodeInstance.setNodeInstanceContainer(this);
                nodeInstance.setProcessInstance(getProcessInstance());
                String uniqueID = (String) node.getMetaData().get("UniqueId");
                if (uniqueID == null) {
                    uniqueID = node.getId() + "";
                }
                int level = this.getLevelForNode(uniqueID);
                nodeInstance.setLevel(level);
            }
            return nodeInstance;
        }
        return super.getNodeInstance(node);
    }

    @Override
    public ContextContainer getContextContainer() {
        return getForEachNode().getCompositeNode();
    }

    private Collection<?> evaluateCollectionExpression(String collectionExpression) {
        Object collection;
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                VariableScope.VARIABLE_SCOPE, collectionExpression);
        if (variableScopeInstance != null) {
            collection = variableScopeInstance.getVariable(collectionExpression);
        } else {
            try {
                collection = MVEL.eval(collectionExpression, new NodeInstanceResolverFactory(this));
            } catch (Throwable t) {
                throw new IllegalArgumentException("Could not find collection " + collectionExpression);
            }
        }
        if (collection == null) {
            return Collections.emptyList();
        }
        if (collection instanceof Collection<?>) {
            return (Collection<?>) collection;
        }
        if (collection.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            Collections.addAll(list, (Object[]) collection);
            return list;
        }
        throw new IllegalArgumentException("Unexpected collection type: " + collection.getClass());
    }

    public class ForEachSplitNodeInstance extends NodeInstanceImpl {

        private static final long serialVersionUID = 510l;

        public ForEachSplitNode getForEachSplitNode() {
            return (ForEachSplitNode) getNode();
        }

        @Override
        public void internalTrigger(io.automatik.engine.api.runtime.process.NodeInstance fromm, String type) {
            triggerTime = new Date();
            String collectionExpression = getForEachNode().getCollectionExpression();
            Collection<?> collection = evaluateCollectionExpression(collectionExpression);
            ((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
            if (collection.isEmpty()) {
                ForEachNodeInstance.this
                        .triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
            } else {
                List<NodeInstance> nodeInstances = new ArrayList<>();
                for (Object o : collection) {
                    String variableName = getForEachNode().getVariableName();
                    NodeInstance nodeInstance = ((NodeInstanceContainer) getNodeInstanceContainer())
                            .getNodeInstance(getForEachSplitNode().getTo().getTo());
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) nodeInstance
                            .resolveContextInstance(VariableScope.VARIABLE_SCOPE, variableName);
                    variableScopeInstance.setVariable(this, variableName, o);
                    nodeInstances.add(nodeInstance);

                    if (getForEachNode().isSequential()) {
                        // for sequential mode trigger only first item from the list
                        sequentialCounter++;
                        break;
                    }
                }
                for (NodeInstance nodeInstance : nodeInstances) {
                    logger.debug("Triggering [{}] in multi-instance loop.", nodeInstance.getNodeId());
                    nodeInstance.trigger(this, getForEachSplitNode().getTo().getToType());
                }
                if (!getForEachNode().isWaitForCompletion()) {
                    ForEachNodeInstance.this.triggerCompleted(
                            io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, false);
                }
            }
        }
    }

    public class ForEachJoinNodeInstance extends NodeInstanceImpl {

        private static final long serialVersionUID = 510l;

        public ForEachJoinNode getForEachJoinNode() {
            return (ForEachJoinNode) getNode();
        }

        public void internalTrigger(io.automatik.engine.api.runtime.process.NodeInstance from, String type) {
            triggerTime = new Date();
            Map<String, Object> tempVariables = new HashMap<>();
            VariableScopeInstance subprocessVariableScopeInstance = null;
            if (getForEachNode().getOutputVariableName() != null) {
                subprocessVariableScopeInstance = (VariableScopeInstance) getContextInstance(VariableScope.VARIABLE_SCOPE);

                Collection<Object> outputCollection = (Collection<Object>) subprocessVariableScopeInstance
                        .getVariable(TEMP_OUTPUT_VAR);
                if (outputCollection == null) {
                    outputCollection = new ArrayList<>();
                }

                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstanceImpl) from)
                        .resolveContextInstance(VariableScope.VARIABLE_SCOPE, getForEachNode().getOutputVariableName());
                Object outputVariable = null;
                if (variableScopeInstance != null) {
                    outputVariable = variableScopeInstance.getVariable(getForEachNode().getOutputVariableName());
                }
                outputCollection.add(outputVariable);

                subprocessVariableScopeInstance.setVariable(this, TEMP_OUTPUT_VAR, outputCollection);
                // add temp collection under actual mi output name for completion condition evaluation
                tempVariables.put(getForEachNode().getOutputVariableName(), outputVariable);
                String outputCollectionName = getForEachNode().getOutputCollectionExpression();
                tempVariables.put(outputCollectionName, outputCollection);
            }
            boolean isCompletionConditionMet = evaluateCompletionCondition(tempVariables);
            if (getForEachNode().isSequential() && !isCompletionConditionMet) {
                String collectionExpression = getForEachNode().getCollectionExpression();
                Collection<?> collection = evaluateCollectionExpression(collectionExpression);

                if (collection.size() > sequentialCounter) {

                    String variableName = getForEachNode().getVariableName();
                    NodeInstance nodeInstance = (NodeInstance) ((NodeInstanceContainer) getNodeInstanceContainer())
                            .getNodeInstance(getForEachNode().getForEachSplitNode().getTo().getTo());
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) nodeInstance
                            .resolveContextInstance(VariableScope.VARIABLE_SCOPE, variableName);
                    variableScopeInstance.setVariable(variableName, new ArrayList<>(collection).get(sequentialCounter));
                    sequentialCounter++;

                    logger.debug("Triggering [{}] in multi-instance loop.", ((NodeInstanceImpl) nodeInstance).getNodeId());
                    ((NodeInstance) nodeInstance).trigger(null, getForEachNode().getForEachSplitNode().getTo().getToType());

                }
            }
            if (getNodeInstanceContainer().getNodeInstances().size() == 1 || isCompletionConditionMet) {
                String outputCollection = getForEachNode().getOutputCollectionExpression();
                if (outputCollection != null) {
                    Collection<?> outputVariable = (Collection<Object>) subprocessVariableScopeInstance
                            .getVariable(TEMP_OUTPUT_VAR);

                    if (getForEachNode().getOutAssociations().isEmpty()) {
                        NodeInstanceContainer parent = (NodeInstanceContainer) ((ForEachNodeInstance) getNodeInstanceContainer())
                                .getNodeInstanceContainer();
                        VariableScopeInstance variableScopeInstance = null;
                        if (parent instanceof NodeInstance) {
                            variableScopeInstance = (VariableScopeInstance) ((NodeInstance) parent)
                                    .resolveContextInstance(VariableScope.VARIABLE_SCOPE, outputCollection);
                        } else {

                            variableScopeInstance = (VariableScopeInstance) ((WorkflowProcessInstance) parent)
                                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
                        }
                        variableScopeInstance.setVariable(this, outputCollection, outputVariable);
                    } else {
                        tempVariables.put(getForEachNode().getOutputVariableName(), outputVariable);

                        for (Iterator<DataAssociation> iterator = getForEachNode().getOutAssociations().iterator(); iterator
                                .hasNext();) {
                            DataAssociation association = iterator.next();
                            if (association.getTransformation() != null) {
                                Transformation transformation = association.getTransformation();
                                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                                if (transformer != null) {

                                    Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                                            tempVariables);
                                    NodeInstanceContainer parent = (NodeInstanceContainer) ((ForEachNodeInstance) getNodeInstanceContainer())
                                            .getNodeInstanceContainer();
                                    VariableScopeInstance vars = null;
                                    if (parent instanceof NodeInstance) {
                                        vars = (VariableScopeInstance) ((NodeInstance) parent)
                                                .resolveContextInstance(VariableScope.VARIABLE_SCOPE, association.getTarget());
                                    } else {

                                        vars = (VariableScopeInstance) ((WorkflowProcessInstance) parent)
                                                .getContextInstance(VariableScope.VARIABLE_SCOPE);
                                    }

                                    if (vars != null && parameterValue != null) {

                                        vars.getVariableScope().validateVariable(getProcessInstance().getProcessName(),
                                                association.getTarget(), parameterValue);

                                        vars.setVariable(this, association.getTarget(), parameterValue);
                                    } else {
                                        logger.warn("Could not find variable scope for variable {}", association.getTarget());
                                        logger.warn("Continuing without setting variable.");
                                    }
                                }
                            } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                                NodeInstanceContainer parent = (NodeInstanceContainer) ((ForEachNodeInstance) getNodeInstanceContainer())
                                        .getNodeInstanceContainer();
                                VariableScopeInstance vars = null;
                                if (parent instanceof NodeInstance) {
                                    vars = (VariableScopeInstance) ((NodeInstance) parent)
                                            .resolveContextInstance(VariableScope.VARIABLE_SCOPE, association.getTarget());
                                } else {

                                    vars = (VariableScopeInstance) ((WorkflowProcessInstance) parent)
                                            .getContextInstance(VariableScope.VARIABLE_SCOPE);
                                }
                                if (vars != null) {
                                    Object value = tempVariables.get(association.getSources().get(0));
                                    if (value == null) {
                                        try {
                                            value = MVEL.eval(association.getSources().get(0),
                                                    new ForEachNodeInstanceResolverFactory(this, tempVariables));
                                        } catch (Throwable t) {
                                            // do nothing
                                        }
                                    }
                                    Variable varDef = vars.getVariableScope().findVariable(association.getTarget());
                                    DataType dataType = varDef.getType();
                                    // exclude java.lang.Object as it is considered unknown type
                                    if (!dataType.getStringType().endsWith("java.lang.Object") &&
                                            !dataType.getStringType().endsWith("Object") && value instanceof String) {
                                        value = dataType.readValue((String) value);
                                    } else {
                                        vars.getVariableScope().validateVariable(getProcessInstance().getProcessName(),
                                                association.getTarget(), value);
                                    }
                                    vars.setVariable(this, association.getTarget(), value);
                                } else {
                                    String output = association.getSources().get(0);
                                    String target = association.getTarget();

                                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                                    if (matcher.find()) {
                                        String paramName = matcher.group(1);

                                        String expression = paramName + " = " + output;
                                        NodeInstanceResolverFactory resolver = new ForEachNodeInstanceResolverFactory(this,
                                                tempVariables);
                                        Serializable compiled = MVEL.compileExpression(expression);
                                        MVEL.executeExpression(compiled, resolver);
                                    } else {
                                        logger.warn("Could not find variable scope for variable {}", association.getTarget());
                                        logger.warn("Continuing without setting variable.");
                                    }
                                }

                            }
                        }
                    }

                }
                ((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
                if (getForEachNode().isWaitForCompletion()) {

                    if (!"true".equals(System.getProperty("jbpm.enable.multi.con"))) {

                        triggerConnection(getForEachJoinNode().getTo());
                    } else {

                        List<Connection> connections = getForEachJoinNode()
                                .getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE);
                        for (Connection connection : connections) {
                            triggerConnection(connection);
                        }
                    }
                }
            }
        }

        private boolean evaluateCompletionCondition(Map<String, Object> tempVariables) {
            String expression = getForEachNode().getCompletionConditionExpression();

            if (expression == null || expression.isEmpty()) {
                return false;
            }
            try {
                Object result = null;
                ExpressionCondition expressionCondition = getForEachNode().getExpressionConition();
                if (expressionCondition != null) {
                    result = expressionCondition.isValid(tempVariables);
                } else {

                    result = MVEL.eval(expression,
                            new ForEachNodeInstanceResolverFactory(this, tempVariables));
                }

                if (!(result instanceof Boolean)) {
                    throw new RuntimeException("Completion condition expression must return boolean values: " + result
                            + " for expression " + expression);
                }
                return ((Boolean) result).booleanValue();
            } catch (Throwable t) {
                throw new IllegalArgumentException("Could not evaluate completion condition  " + expression, t);
            }
        }
    }

    @Override
    public ContextInstance getContextInstance(String contextId) {
        ContextInstance contextInstance = super.getContextInstance(contextId);
        if (contextInstance == null) {
            contextInstance = resolveContextInstance(contextId, TEMP_OUTPUT_VAR);
            setContextInstance(contextId, contextInstance);
        }

        return contextInstance;
    }

    @Override
    public int getLevelForNode(String uniqueID) {
        // always 1 for for each
        return 1;
    }

    public void setInternalSequentialCounter(int counter) {
        this.sequentialCounter = counter;
    }

    public int getSequentialCounter() {
        return this.sequentialCounter;
    }

    private class ForEachNodeInstanceResolverFactory extends NodeInstanceResolverFactory {

        private static final long serialVersionUID = -8856846610671009685L;

        private Map<String, Object> tempVariables;

        public ForEachNodeInstanceResolverFactory(NodeInstance nodeInstance, Map<String, Object> tempVariables) {
            super(nodeInstance);
            this.tempVariables = tempVariables;
        }

        @Override
        public boolean isResolveable(String name) {
            boolean result = tempVariables.containsKey(name);
            if (result) {
                return result;
            }
            return super.isResolveable(name);
        }

        @Override
        public VariableResolver getVariableResolver(String name) {
            if (tempVariables.containsKey(name)) {
                return new SimpleValueResolver(tempVariables.get(name));
            }
            return super.getVariableResolver(name);
        }
    }
}
