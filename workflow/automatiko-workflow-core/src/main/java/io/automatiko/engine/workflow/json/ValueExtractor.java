package io.automatiko.engine.workflow.json;

import com.fasterxml.jackson.databind.JsonNode;

public class ValueExtractor {

    public static Object extract(Object value, Class<?> expectedType) {

        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (value instanceof JsonNode) {
            if (expectedType == String.class) {
                return ((JsonNode) value).asText();
            }
            if (expectedType == Boolean.class) {
                return ((JsonNode) value).asBoolean();
            }
            if (expectedType == Integer.class) {
                return ((JsonNode) value).asInt();
            }
            if (expectedType == Long.class) {
                return ((JsonNode) value).asLong();
            }
        }

        return value;
    }
}
