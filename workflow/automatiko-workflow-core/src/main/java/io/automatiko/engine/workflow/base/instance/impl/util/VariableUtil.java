package io.automatiko.engine.workflow.base.instance.impl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

public class VariableUtil {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String resolveVariable(String s, NodeInstance nodeInstance) {
        if (s == null) {
            return null;
        }

        Map<String, String> replacements = new HashMap<String, String>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacementKey = paramName;
            String defaultValue = "";
            if (paramName.contains(":")) {

                String[] items = paramName.split(":");
                paramName = items[0];
                defaultValue = items[1];
            }

            if (replacements.get(paramName) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance)
                        .resolveContextInstance(VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                    replacements.put(replacementKey, variableValueString);
                } else {
                    try {
                        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) nodeInstance
                                .getProcessInstance()
                                .getProcess())
                                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                        Object variableValue = evaluator.evaluate(paramName, new NodeInstanceResolverFactory(
                                (io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance));
                        String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                        replacements.put(replacementKey, variableValueString);
                    } catch (Throwable t) {

                    }
                }
            }
        }
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            s = s.replace("#{" + replacement.getKey() + "}", replacement.getValue());
        }

        return s;
    }

    public static String nameFromDotNotation(String value) {
        if (value == null) {
            return null;
        }

        if (value.contains(".")) {
            return value.split("\\.")[0];
        }

        if (value.endsWith("[]")) {
            return value.substring(0, value.length() - 2);
        }
        if (value.endsWith("[+]") || value.endsWith("[-]")) {
            return value.substring(0, value.length() - 3);
        }

        return value;
    }

    public static String transformDotNotation(String value, String object) {
        if (value == null || !value.contains(".")) {
            if (value.endsWith("[]")) {
                StringBuilder expression = new StringBuilder(value.substring(0, value.length() - 2) + ".");
                // considered ass add element to collection
                expression.append("add").append("(").append(object).append(")");
                return expression.toString();
            } else if (value.endsWith("[+]")) {
                StringBuilder expression = new StringBuilder(value.substring(0, value.length() - 3) + ".");
                // considered ass add element to collection
                expression.append("add").append("(").append(object).append(")");
                return expression.toString();
            } else if (value.endsWith("[-]")) {
                StringBuilder expression = new StringBuilder(value.substring(0, value.length() - 3) + ".");
                // considered ass add element to collection
                expression.append("remove").append("(").append(object).append(")");
                return expression.toString();
            } else {
                StringBuilder expression = new StringBuilder(value);
                expression.append("=").append(object);

                return expression.toString();
            }
        }

        String[] items = value.split("\\.");

        String setter = items[items.length - 1];
        StringBuilder expression = new StringBuilder(items[0] + ".");
        for (int i = 1; i < items.length - 1; i++) {
            expression.append("get").append(StringUtils.capitalize(items[i])).append("().");
        }
        if (setter.endsWith("[]")) {
            // considered ass add element to collection
            if (items.length == 2) {
                expression.append("get").append(StringUtils.capitalize(setter.substring(0, setter.length() - 2)))
                        .append("().");
            }
            expression.append("add").append("(").append(object).append(")");
        } else if (setter.endsWith("[]") || setter.endsWith("[+]")) {
            // considered ass add element to collection
            if (items.length == 2) {
                expression.append("get").append(StringUtils.capitalize(setter.substring(0, setter.length() - 3)))
                        .append("().");
            }
            expression.append("add").append("(").append(object).append(")");
        } else if (setter.endsWith("[-]")) {
            if (items.length == 2) {
                expression.append("get").append(StringUtils.capitalize(setter.substring(0, setter.length() - 3)))
                        .append("().");
            }
            expression.append("remove").append("(").append(object).append(")");
        } else {
            expression.append("set").append(StringUtils.capitalize(setter)).append("(").append(object).append(")");
        }
        return expression.toString();
    }

}
