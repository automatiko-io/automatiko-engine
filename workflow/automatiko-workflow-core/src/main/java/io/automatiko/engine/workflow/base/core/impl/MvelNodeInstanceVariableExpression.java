package io.automatiko.engine.workflow.base.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.VariableExpression;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

public class MvelNodeInstanceVariableExpression implements VariableExpression, Serializable {

    private static final long serialVersionUID = 3100263402644609014L;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> T evaluate(String expression, ProcessContext context, Class<T> clazz) {

        Map<String, Object> replacements = new HashMap<>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(expression);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacementKey = paramName;
            String defaultValue = null;
            if (paramName.contains(":")) {

                String[] items = paramName.split(":");
                paramName = items[0];
                defaultValue = items[1];
            }
            if (replacements.get(paramName) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) context.getNodeInstance())
                        .resolveContextInstance(VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                    replacements.put(replacementKey, variableValueString);
                } else {
                    try {
                        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                                .getProcess())
                                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                        Object variableValue = evaluator.evaluate(paramName,
                                resolverFactory((NodeInstance) context.getNodeInstance()));

                        replacements.put(replacementKey, variableValue == null ? defaultValue : variableValue);
                    } catch (Throwable t) {
                        replacements.put(replacementKey, defaultValue);
                    }
                }
            }
        }
        for (Map.Entry<String, Object> replacement : replacements.entrySet()) {
            expression = expression.replace("#{" + replacement.getKey() + "}", replacement.getValue().toString());
        }

        if (expression == null) {
            return null;
        }

        if (clazz.isInstance(expression)) {
            return clazz.cast(expression);
        }

        if (clazz == String.class) {
            return (T) expression.toString();
        }

        throw new IllegalStateException("Variable expression evaluated to non compatible type with " + clazz);
    }

    protected NodeInstanceResolverFactory resolverFactory(NodeInstance nodeInstance) {
        return new NodeInstanceResolverFactory(nodeInstance);
    }
}
