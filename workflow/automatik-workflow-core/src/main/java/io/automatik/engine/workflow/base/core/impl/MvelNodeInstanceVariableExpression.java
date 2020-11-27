package io.automatik.engine.workflow.base.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.workflow.base.core.VariableExpression;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

public class MvelNodeInstanceVariableExpression implements VariableExpression, Serializable {

    private static final long serialVersionUID = 3100263402644609014L;

    @Override
    public <T> T evaluate(String expression, ProcessContext context, Class<T> clazz) {

        Map<String, Object> replacements = new HashMap<>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(expression);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (replacements.get(paramName) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((NodeInstance) context.getNodeInstance())
                        .resolveContextInstance(VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? "" : variableValue.toString();
                    replacements.put(paramName, variableValueString);
                } else {
                    try {
                        Object variableValue = MVEL.eval(paramName,
                                resolverFactory((NodeInstance) context.getNodeInstance()));

                        replacements.put(paramName, variableValue);
                    } catch (Throwable t) {
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
