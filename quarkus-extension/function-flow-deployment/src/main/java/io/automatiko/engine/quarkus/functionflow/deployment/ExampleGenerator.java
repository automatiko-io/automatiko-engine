package io.automatiko.engine.quarkus.functionflow.deployment;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.util.Json;

/**
 * 
 * Inspired by OpenAPI tools implementation of example generator
 *
 */
public class ExampleGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleGenerator.class);

    private static final String URL = "url";
    private static final String URI = "uri";

    private Random random;

    public ExampleGenerator() {

        // use a fixed seed to make the "random" numbers reproducible.
        this.random = new Random("ExampleGenerator".hashCode());
    }

    public String generate(Schema property, OpenAPI openApi) {
        LOGGER.debug("debugging generate in ExampleGenerator");
        Set<String> processedModels = new HashSet<>();

        Map<String, Object> kv = new LinkedHashMap<>();

        for (Entry<String, Schema> prop : property.getProperties().entrySet()) {

            Object example = resolvePropertyToExample(prop.getKey(), prop.getValue(), processedModels, openApi);
            if (example != null) {

                kv.put(prop.getKey(), example);

            }
        }
        return Json.pretty(kv);
    }

    private Object resolvePropertyToExample(String propertyName, Schema property,
            Set<String> processedModels, OpenAPI openApi) {
        LOGGER.debug("Resolving example for property {}...", property);

        if (property.getRef() != null) {
            property = openApi.getComponents().getSchemas().get(getSimpleRef(property.getRef()));
        }

        if (property.getType().equals(SchemaType.BOOLEAN)) {
            Object defaultValue = property.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            return Boolean.TRUE;
        } else if (property.getType().equals(SchemaType.ARRAY)) {
            Schema innerType = property.getItems();
            if (innerType != null) {
                int arrayLength = 2;
                // avoid memory issues by limiting to max. 5 items
                arrayLength = Math.min(arrayLength, 5);
                Object[] objectProperties = new Object[arrayLength];
                Object objProperty = resolvePropertyToExample(propertyName, innerType, processedModels, openApi);
                for (int i = 0; i < arrayLength; i++) {
                    objectProperties[i] = objProperty;
                }
                return objectProperties;
            }
        } else if (property.getType().equals(SchemaType.NUMBER)) {
            Double min = getPropertyValue(property.getMinimum());
            Double max = getPropertyValue(property.getMaximum());

            return randomNumber(min, max);

        } else if (property.getType().equals(SchemaType.STRING)) {
            LOGGER.debug("String property");
            String defaultValue = (String) property.getDefaultValue();
            if (defaultValue != null && !defaultValue.isEmpty()) {
                LOGGER.debug("Default value found: '{}'", defaultValue);
                return defaultValue;
            }
            List<Object> enumValues = property.getEnumeration();
            if (enumValues != null && !enumValues.isEmpty()) {
                LOGGER.debug("Enum value found: '{}'", enumValues.get(0));
                return enumValues.get(0);
            }
            String format = property.getFormat();
            if (format != null && (URI.equals(format) || URL.equals(format))) {
                LOGGER.debug("URI or URL format, without default or enum, generating random one.");
                return "http://example.com/aeiou";
            }
            LOGGER.debug("No values found, using property name " + propertyName + " as example");
            return "string";
        } else if (property.getType().equals(SchemaType.OBJECT)) {
            return generate(property, openApi);
        }

        return "";
    }

    private Double getPropertyValue(BigDecimal propertyValue) {
        return propertyValue == null ? null : propertyValue.doubleValue();
    }

    private double randomNumber(Double min, Double max) {
        if (min != null && max != null) {
            double range = max - min;
            return random.nextDouble() * range + min;
        } else if (min != null) {
            return random.nextDouble() + min;
        } else if (max != null) {
            return random.nextDouble() * max;
        } else {
            return random.nextDouble() * 10;
        }
    }

    private String getSimpleRef(String ref) {
        if (ref.startsWith("#/components/")) {
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        } else if (ref.startsWith("#/definitions/")) {
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        } else {
            return null;

        }

        try {
            ref = URLDecoder.decode(ref, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        // see https://tools.ietf.org/html/rfc6901#section-3
        // Because the characters '~' (%x7E) and '/' (%x2F) have special meanings in
        // JSON Pointer, '~' needs to be encoded as '~0' and '/' needs to be encoded 
        // as '~1' when these characters appear in a reference token.
        // This reverses that encoding.
        ref = ref.replace("~1", "/").replace("~0", "~");

        return ref;
    }
}
