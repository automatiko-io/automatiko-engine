package io.automatik.engine.workflow.base.instance.impl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.util.PatternConstants;

public class VariableUtil {

    public static String resolveVariable(String s, NodeInstance nodeInstance) {
        if (s == null) {
            return null;
        }

        Map<String, String> replacements = new HashMap<String, String>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (replacements.get(paramName) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((io.automatik.engine.workflow.process.instance.NodeInstance) nodeInstance)
                        .resolveContextInstance(VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? "" : variableValue.toString();
                    replacements.put(paramName, variableValueString);
                } else {
                    try {
                        Object variableValue = MVEL.eval(paramName, new NodeInstanceResolverFactory(
                                (io.automatik.engine.workflow.process.instance.NodeInstance) nodeInstance));
                        String variableValueString = variableValue == null ? "" : variableValue.toString();
                        replacements.put(paramName, variableValueString);
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
            }
            return value;
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
