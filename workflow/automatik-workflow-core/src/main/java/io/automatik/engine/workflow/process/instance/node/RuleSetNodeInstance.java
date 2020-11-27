
package io.automatik.engine.workflow.process.instance.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.decision.DecisionModel;
import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatik.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatik.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.core.node.RuleSetNode;
import io.automatik.engine.workflow.process.core.node.Transformation;
import io.automatik.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of a ruleset node.
 */
public class RuleSetNodeInstance extends StateBasedNodeInstance implements EventListener, ContextInstanceContainer {

    private static final long serialVersionUID = 510L;
    private static final Logger logger = LoggerFactory.getLogger(RuleSetNodeInstance.class);

    // NOTE: ContetxInstances are not persisted as current functionality (exception
    // scope) does not require it
    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<>();

    protected RuleSetNode getRuleSetNode() {
        return (RuleSetNode) getNode();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void internalTrigger(final NodeInstance from, String type) {
        try {
            super.internalTrigger(from, type);
            // if node instance was cancelled, abort
            if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
                return;
            }
            if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
                throw new IllegalArgumentException("A RuleSetNode only accepts default incoming connections!");
            }
            RuleSetNode ruleSetNode = getRuleSetNode();

            Map<String, Object> inputs = evaluateParameters(ruleSetNode);

            RuleSetNode.RuleType ruleType = ruleSetNode.getRuleType();
            if (ruleType.isDecision()) {
                RuleSetNode.RuleType.Decision decisionModel = (RuleSetNode.RuleType.Decision) ruleType;
                String dName = resolveVariable(decisionModel.getDecision());

                DecisionModel modelInstance = getRuleSetNode().getDecisionModel().get();

                Object context = modelInstance.newContext(inputs);
                Object dmnResult = null;
                if (dName == null) {
                    dmnResult = modelInstance.evaluateAll(context);
                } else if (decisionModel.isDecisionService()) {
                    dmnResult = modelInstance.evaluateDecisionService(context, dName);
                } else {
                    dmnResult = modelInstance.evaluateDecisionByName(context, dName);
                }

                if (modelInstance.hasErrors(dmnResult)) {

                    throw new RuntimeException("DMN result errors:: " + modelInstance.buildErrorMessage(dmnResult));
                }
                processOutputs(inputs, modelInstance.getResultData(dmnResult));
                triggerCompleted();
            } else {
                throw new UnsupportedOperationException("Unsupported Rule Type: " + ruleType);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Throwable e) {
        ExceptionScopeInstance exceptionScopeInstance = getExceptionScopeInstance(e);
        if (exceptionScopeInstance != null) {
            exceptionScopeInstance.handleException(this, e.getClass().getName(), e);
        } else {
            Throwable rootCause = getRootException(e);
            if (rootCause != null) {
                exceptionScopeInstance = getExceptionScopeInstance(rootCause);
                if (exceptionScopeInstance != null) {
                    exceptionScopeInstance.handleException(this, rootCause.getClass().getName(), rootCause);

                    return;
                }
            }
            throw new WorkflowRuntimeException(this, getProcessInstance(),
                    "Unable to execute Action: " + e.getMessage(), e);
        }
    }

    private ExceptionScopeInstance getExceptionScopeInstance(Throwable e) {
        return (ExceptionScopeInstance) resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, e.getClass().getName());
    }

    protected Throwable getRootException(Throwable exception) {
        Throwable rootException = exception;
        while (rootException.getCause() != null) {
            rootException = rootException.getCause();
        }
        return rootException;
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    private void processOutputs(Map<String, Object> inputs, Map<String, Object> objects) {
        RuleSetNode ruleSetNode = getRuleSetNode();
        if (ruleSetNode != null) {
            for (Iterator<DataAssociation> iterator = ruleSetNode.getOutAssociations().iterator(); iterator
                    .hasNext();) {
                DataAssociation association = iterator.next();
                if (association.getTransformation() != null) {
                    Transformation transformation = association.getTransformation();
                    DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                    if (transformer != null) {
                        Map<String, Object> dataSet = new HashMap<String, Object>();
                        dataSet.putAll(inputs);
                        dataSet.putAll(objects);

                        Object parameterValue = transformer.transform(transformation.getCompiledExpression(), dataSet);

                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VariableScope.VARIABLE_SCOPE, association.getTarget());
                        if (variableScopeInstance != null && parameterValue != null) {
                            variableScopeInstance.setVariable(this, association.getTarget(), parameterValue);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                            logger.warn("Continuing without setting variable.");
                        }
                    }
                } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VariableScope.VARIABLE_SCOPE, association.getTarget());
                    if (variableScopeInstance != null) {
                        Object value = objects.get(association.getSources().get(0));
                        if (value == null) {
                            try {
                                value = MVEL.eval(association.getSources().get(0),
                                        new MapVariableResolverFactory(objects));
                            } catch (Throwable t) {
                                // do nothing
                            }
                        }
                        Variable varDef = variableScopeInstance.getVariableScope()
                                .findVariable(association.getTarget());
                        DataType dataType = varDef.getType();
                        // exclude java.lang.Object as it is considered unknown type
                        if (!dataType.getStringType().endsWith("java.lang.Object") && value instanceof String) {
                            value = dataType.readValue((String) value);
                        }
                        variableScopeInstance.setVariable(this, association.getTarget(), value);
                    } else {
                        String output = association.getSources().get(0);
                        String target = association.getTarget();

                        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                        if (matcher.find()) {
                            String paramName = matcher.group(1);

                            String expression = VariableUtil.transformDotNotation(paramName, output);
                            NodeInstanceResolverFactory resolver = new NodeInstanceResolverFactory(this);
                            resolver.addExtraParameters(objects);
                            Serializable compiled = MVEL.compileExpression(expression);
                            MVEL.executeExpression(compiled, resolver);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                        }
                    }
                }
            }
        }
    }

    protected Map<String, Object> evaluateParameters(RuleSetNode ruleSetNode) {
        Map<String, Object> replacements = new HashMap<>();

        for (Iterator<DataAssociation> iterator = ruleSetNode.getInAssociations().iterator(); iterator.hasNext();) {
            DataAssociation association = iterator.next();
            if (association.getTransformation() != null) {
                Transformation transformation = association.getTransformation();
                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                if (transformer != null) {
                    Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                            getSourceParameters(association));

                    replacements.put(association.getTarget(), parameterValue);
                }
            } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                Object parameterValue = null;
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, association.getSources().get(0));
                if (variableScopeInstance != null) {
                    parameterValue = variableScopeInstance.getVariable(association.getSources().get(0));
                } else {
                    try {
                        parameterValue = MVEL.eval(association.getSources().get(0),
                                new NodeInstanceResolverFactory(this));
                    } catch (Throwable t) {
                        logger.error("Could not find variable scope for variable {}", association.getSources().get(0));
                        logger.error("when trying to execute RuleSetNode {}", ruleSetNode.getName());
                        logger.error("Continuing without setting parameter.");
                    }
                }
                replacements.put(association.getTarget(), parameterValue);

            }
        }

        for (Map.Entry<String, Object> entry : ruleSetNode.getParameters().entrySet()) {
            if (entry.getValue() instanceof String) {

                Object value = resolveVariable(entry.getValue());

                replacements.put(entry.getKey(), value);
            }
        }

        return replacements;
    }

    private Object resolveVariable(Object s) {

        if (s instanceof String) {
            Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher((String) s);
            while (matcher.find()) {
                String paramName = matcher.group(1);

                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    if (variableValue != null) {
                        return variableValue;
                    }
                } else {
                    try {
                        Object variableValue = MVEL.eval(paramName, new NodeInstanceResolverFactory(this));
                        if (variableValue != null) {
                            return variableValue;
                        }
                    } catch (Throwable t) {
                        logger.error("Could not find variable scope for variable {}", paramName);
                    }
                }
            }
        }

        return s;
    }

    protected Map<String, Object> getSourceParameters(DataAssociation association) {
        Map<String, Object> parameters = new HashMap<>();
        for (String sourceParam : association.getSources()) {
            Object parameterValue = null;
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                    VariableScope.VARIABLE_SCOPE, sourceParam);
            if (variableScopeInstance != null) {
                parameterValue = variableScopeInstance.getVariable(sourceParam);
            } else {
                try {
                    parameterValue = MVEL.eval(sourceParam, new NodeInstanceResolverFactory(this));
                } catch (Throwable t) {
                    logger.warn("Could not find variable scope for variable {}", sourceParam);
                }
            }
            if (parameterValue != null) {
                parameters.put(sourceParam, parameterValue);
            }
        }

        return parameters;
    }

    @Override
    public List<ContextInstance> getContextInstances(String contextId) {
        return this.subContextInstances.get(contextId);
    }

    @Override
    public void addContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.computeIfAbsent(contextId, key -> new ArrayList<>());
        list.add(contextInstance);
    }

    @Override
    public void removeContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list != null) {
            list.remove(contextInstance);
        }
    }

    @Override
    public ContextInstance getContextInstance(String contextId, long id) {
        List<ContextInstance> contextInstances = subContextInstances.get(contextId);
        if (contextInstances != null) {
            for (ContextInstance contextInstance : contextInstances) {
                if (contextInstance.getContextId() == id) {
                    return contextInstance;
                }
            }
        }
        return null;
    }

    @Override
    public ContextInstance getContextInstance(Context context) {
        ContextInstanceFactory conf = ContextInstanceFactoryRegistry.INSTANCE.getContextInstanceFactory(context);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal context type (registry not found): " + context.getClass());
        }
        ContextInstance contextInstance = conf.getContextInstance(context, this, getProcessInstance());
        if (contextInstance == null) {
            throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
        }
        return contextInstance;
    }

    @Override
    public ContextContainer getContextContainer() {
        return getRuleSetNode();
    }
}
