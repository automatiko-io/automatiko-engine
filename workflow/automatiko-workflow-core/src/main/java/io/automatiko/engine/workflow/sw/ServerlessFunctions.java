package io.automatiko.engine.workflow.sw;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.event.AbstractDataEvent;
import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ServerlessFunctions implements Functions {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void inject(ProcessContext context, String json) {

        try {
            ObjectNode data = (ObjectNode) mapper.readTree(json);
            data.fields().forEachRemaining(field -> context.setVariable(field.getKey(), field.getValue()));
        } catch (Exception e) {
            throw new RuntimeException("Error injecting data into state data", e);
        }
    }

    public static Object expression(ProcessContext context, String expression) {

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
        Map<String, Object> vars = new HashMap<>();
        vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
        if (context.getVariable("$CONST") != null) {
            vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
        }
        Object content = evaluator.evaluate(expression, vars);

        return content;
    }

    public static String expressionAsString(ProcessContext context, String expression) {

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
        Map<String, Object> vars = new HashMap<>();
        vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
        if (context.getVariable("$CONST") != null) {
            vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
        }
        Object content = evaluator.evaluate(expression, vars);

        if (content == null) {
            return null;
        }
        if (content instanceof TextNode) {
            return ((TextNode) content).asText();
        }

        return content.toString();
    }

    public static void expression(ProcessContext context, String expression, String inputFilter) {

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
        Map<String, Object> vars = new HashMap<>();
        vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
        if (context.getVariable("$CONST") != null) {
            vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
        }

        if (inputFilter != null) {
            Object filteredInput = evaluator.evaluate(inputFilter, vars);
            vars.put("workflowdata", filteredInput);
        }
        JsonNode outcome = (JsonNode) evaluator.evaluate(expression, vars);
        Object updated;
        try {
            if (outcome != null) {
                updated = mapper.readerForUpdating(context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY))
                        .readValue(outcome);
                context.setVariable(JsonVariableScope.WORKFLOWDATA_KEY, updated);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Object expression(ProcessContext context, String expression, String inputFilter, String outputFilter,
            String scope) {

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
        Map<String, Object> vars = new HashMap<>();
        Map<String, Object> variables = new HashMap<>();
        vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
        if (context.getVariable("$CONST") != null) {
            variables.put("CONST", context.getVariable("$CONST"));
            vars.put("workflow_variables", variables);
        }

        if (inputFilter != null) {
            Object filteredInput = evaluator.evaluate(inputFilter, vars);
            vars.put("workflowdata", filteredInput);
        }
        Object content = evaluator.evaluate(expression, vars);

        if (outputFilter != null) {
            vars.put("workflowdata", content);
            content = evaluator.evaluate(outputFilter, vars);

        }
        JsonNode outcome;
        if (scope != null) {
            vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
            variables.put("v", content);
            vars.put("workflow_variables", variables);
            outcome = (JsonNode) evaluator.evaluate(scope + "=$v", vars);

        } else {
            outcome = (JsonNode) context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY);
        }

        try {
            Object updated = mapper.readerForUpdating(context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY))
                    .readValue(outcome);

            context.setVariable(JsonVariableScope.WORKFLOWDATA_KEY, updated);

            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Object variableExpression(ProcessContext context, String expression) {

        if (expression.startsWith("${") || expression.startsWith("\"${")) {

            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                    .getProcess())
                            .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
            Map<String, Object> vars = new HashMap<>();
            vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
            if (context.getVariable("$CONST") != null) {
                vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
            }
            Object content = evaluator.evaluate(unwrapExpression(expression), vars);

            return content;
        } else {
            return fromJson(expression);
        }
    }

    public static JsonNode fromJson(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Error transforming json string to object", e);
        }
    }

    public static String extensionAttribute(AbstractDataEvent<?> eventData, String attributeName) {
        String actualAttributeName = eventData.getExtensions().keySet().stream()
                .filter(attr -> attr.equalsIgnoreCase(attributeName)).findFirst().orElse(null);
        if (actualAttributeName != null) {
            return eventData.getExtension(actualAttributeName).toString();
        }

        return null;
    }

    public static boolean hasAttributeWithValue(AbstractDataEvent<?> eventData, String attributeName, String value) {

        if (attributeName == null) {
            return false;
        }
        if (attributeName.equalsIgnoreCase("id")) {
            return eventData.getId() == null ? false : eventData.getId().equalsIgnoreCase(value);
        } else if (attributeName.equalsIgnoreCase("type")) {
            return eventData.getType() == null ? false : eventData.getType().equalsIgnoreCase(value);
        } else if (attributeName.equalsIgnoreCase("source")) {
            return eventData.getSource() == null ? false : eventData.getSource().equalsIgnoreCase(value);
        } else if (attributeName.equalsIgnoreCase("subject")) {
            return eventData.getSubject() == null ? false : eventData.getSubject().equalsIgnoreCase(value);
        } else if (attributeName.equalsIgnoreCase("time")) {
            return eventData.getTime() == null ? false : eventData.getTime().equalsIgnoreCase(value);
        }

        String actualAttributeName = eventData.getExtensions().keySet().stream()
                .filter(attr -> attr.equalsIgnoreCase(attributeName)).findFirst().orElse(null);
        if (actualAttributeName != null) {
            return eventData.getExtension(actualAttributeName).toString().equalsIgnoreCase(value);
        }

        return false;
    }

    private static String unwrapExpression(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (input.startsWith("${")) {
            return trimmed.trim().substring(2, trimmed.length() - 2);
        } else if (input.startsWith("\"${")) {
            return trimmed.trim().substring(3, trimmed.length() - 3);
        } else if (input.startsWith("\"")) {
            return trimmed.trim().substring(1, trimmed.length() - 1);
        }
        return input.trim();
    }

    public static <T> T fromJsonNode(JsonNode json, Class<T> clazz) {
        try {
            return mapper.convertValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error transforming json node to object", e);
        }
    }

    public static JsonNode toJsonNode(Object value) {
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }

        try {
            return mapper.valueToTree(value);
        } catch (Exception e) {
            throw new RuntimeException("Error transforming object to json node", e);
        }
    }

    public static void copyData(Map<String, Object> values, ProcessContext context) {
        if (values != null) {
            for (Entry<String, Object> entry : values.entrySet()) {
                context.setVariable(entry.getKey(), toJsonNode(entry.getValue()));
            }
        }
    }

    public static void convertData(Map<String, Object> values, Model model) {
        if (values != null) {
            Map<String, Object> valuesConverted = new HashMap<>();
            try {
                BeanInfo info = Introspector.getBeanInfo(model.getClass());

                for (Entry<String, Object> entry : values.entrySet()) {

                    if (entry.getValue() instanceof JsonNode) {
                        Class<?> type = Stream.of(info.getPropertyDescriptors()).filter(p -> p.getName().equals(entry.getKey()))
                                .map(p -> p.getPropertyType()).findFirst().get();

                        Object converted = fromJsonNode((JsonNode) entry.getValue(), type);
                        valuesConverted.put(entry.getKey(), converted);
                    } else {
                        valuesConverted.put(entry.getKey(), entry.getValue());
                    }
                }

                model.fromMap(valuesConverted);
            } catch (IntrospectionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
