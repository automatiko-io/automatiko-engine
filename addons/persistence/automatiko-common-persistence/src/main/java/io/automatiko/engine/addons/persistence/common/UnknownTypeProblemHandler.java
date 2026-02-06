package io.automatiko.engine.addons.persistence.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class UnknownTypeProblemHandler extends DeserializationProblemHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnknownTypeProblemHandler.class);

    private Map<String, Class<?>> unknownTypesMapping = new HashMap<>();

    public UnknownTypeProblemHandler() {
        try {
            Enumeration<URL> mappingFiles = this.getClass().getClassLoader()
                    .getResources("/automatiko-persistence-mapping.properties");

            while (mappingFiles.hasMoreElements()) {
                URL url = (URL) mappingFiles.nextElement();

                try (InputStream is = url.openStream()) {
                    Properties props = new Properties();
                    props.load(is);

                    for (Entry<Object, Object> entry : props.entrySet()) {
                        try {
                            Class<?> clazz = Class.forName(entry.getValue().toString());
                            unknownTypesMapping.put(entry.getKey().toString(), clazz);
                        } catch (ClassNotFoundException e) {
                            LOGGER.warn("Mapping of {} to {} cannot be converted to class object due to {}", entry.getKey(),
                                    entry.getValue(), e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to load persistence mapping files", e);
        }
    }

    @Override
    public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId,
            TypeIdResolver idResolver, String failureMsg) throws IOException {
        if (unknownTypesMapping.containsKey(subTypeId)) {

            return TypeFactory.defaultInstance().constructType(unknownTypesMapping.get(subTypeId));
        }
        return super.handleUnknownTypeId(ctxt, baseType, subTypeId, idResolver, failureMsg);
    }

}
