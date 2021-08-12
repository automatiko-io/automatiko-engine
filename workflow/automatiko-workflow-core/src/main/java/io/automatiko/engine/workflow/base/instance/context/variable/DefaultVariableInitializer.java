package io.automatiko.engine.workflow.base.instance.context.variable;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.workflow.Variable;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.util.PatternConstants;

public class DefaultVariableInitializer implements VariableInitializer {

    @Override
    public Object initialize(Process process, Variable definition, Map<String, Object> data) {

        String valueExpression = (String) definition
                .getMetaData(io.automatiko.engine.workflow.base.core.context.variable.Variable.DEFAULT_VALUE);

        if (valueExpression == null) {
            Class<?> clazz = definition.getType().getClassType();
            if (List.class.isAssignableFrom(clazz)) {
                return new ArrayList<>();
            } else if (Set.class.isAssignableFrom(clazz)) {
                return new LinkedHashSet<>();
            } else if (Map.class.isAssignableFrom(clazz)) {
                return new HashMap<>();
            } else if (String.class.isAssignableFrom(clazz)) {
                return "";
            } else if (Boolean.class.isAssignableFrom(clazz)) {
                return false;
            } else if (Integer.class.isAssignableFrom(clazz)) {
                return 0;
            } else if (Long.class.isAssignableFrom(clazz)) {
                return Long.valueOf(0);
            } else if (Double.class.isAssignableFrom(clazz)) {
                return Double.valueOf(0);
            } else if (Float.class.isAssignableFrom(clazz)) {
                return Float.valueOf(0);
            } else if (Date.class.isAssignableFrom(clazz)) {
                return new Date();
            } else if (LocalDate.class.isAssignableFrom(clazz)) {
                return LocalDate.now();
            } else if (LocalDateTime.class.isAssignableFrom(clazz)) {
                return LocalDateTime.now();
            } else {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    throw new RuntimeException("Unable to initialize variable of type " + clazz, e);
                }
            }
        }

        return defaultValue(process, valueExpression, definition, data);
    }

    protected Object defaultValue(Process process, String valueExpression, Variable definition, Map<String, Object> data) {
        try {
            Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(valueExpression);

            if (matcher.find()) {
                String paramName = matcher.group(1);
                String defaultValue = null;
                if (paramName.contains(":")) {

                    String[] items = paramName.split(":");
                    paramName = items[0];
                    defaultValue = items[1];
                }
                ExpressionEvaluator<?> evaluator = (ExpressionEvaluator<?>) ((WorkflowProcess) process)
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                Object result = evaluator.evaluate(paramName, data);

                return result == null ? defaultValue : result;

            } else {
                return definition.getType().readValue(valueExpression);
            }
        } catch (Throwable e) {
            return null;
        }
    }

}
