package io.automatiko.engine.workflow.base.instance.context.variable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatiko.engine.api.workflow.Variable;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.workflow.util.PatternConstants;

public class DefaultVariableInitializer implements VariableInitializer {

    @Override
    public Object initialize(Variable definition, Map<String, Object> data) {

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
            } else {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    throw new RuntimeException("Unable to initialize variable of type " + clazz, e);
                }
            }
        }

        return defaultValue(valueExpression, definition, data);
    }

    protected Object defaultValue(String valueExpression, Variable definition, Map<String, Object> data) {
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(valueExpression);

        if (matcher.find()) {
            String paramName = matcher.group(1);
            String defaultValue = null;
            if (paramName.contains(":")) {

                String[] items = paramName.split(":");
                paramName = items[0];
                defaultValue = items[1];
            }

            Object result = MVEL.eval(paramName, data);

            return result == null ? defaultValue : result;

        } else {
            return definition.getType().readValue(valueExpression);
        }
    }

}
