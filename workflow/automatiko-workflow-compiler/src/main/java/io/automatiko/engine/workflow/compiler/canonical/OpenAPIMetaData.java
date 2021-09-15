package io.automatiko.engine.workflow.compiler.canonical;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.automatiko.engine.services.utils.IoUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIMetaData {

    private static Map<String, OpenAPIMetaData> cached = new ConcurrentHashMap<String, OpenAPIMetaData>();

    private final String name;

    private final OpenAPI api;

    private final Set<String> operations = new LinkedHashSet<String>();

    private final Map<String, Map<String, String>> operationsParameters = new HashMap<>();

    private OpenAPIMetaData(String name, OpenAPI api) {
        this.name = name;
        this.api = api;
    }

    public String name() {
        return name;
    }

    public OpenAPI api() {
        return api;
    }

    public Set<String> operations() {
        return this.operations;
    }

    public void addOperation(String operation, Map<String, String> params) {
        AtomicBoolean foundInApi = new AtomicBoolean(false);

        api.getPaths().values().forEach(path -> {
            boolean hasGet = checkOperationById(path.getGet(), operation);
            boolean hasPost = checkOperationById(path.getPost(), operation);
            boolean hasDelete = checkOperationById(path.getDelete(), operation);
            boolean hasHead = checkOperationById(path.getHead(), operation);
            boolean hasOptions = checkOperationById(path.getOptions(), operation);
            boolean hasPatch = checkOperationById(path.getPatch(), operation);
            boolean hasPut = checkOperationById(path.getPut(), operation);
            boolean hasTrace = checkOperationById(path.getTrace(), operation);

            if (hasGet || hasPost || hasDelete || hasHead || hasOptions || hasPatch || hasPut || hasTrace) {
                foundInApi.set(true);
            }
        });

        if (!foundInApi.get()) {
            throw new IllegalArgumentException(
                    "There is no api operation with operation id '" + operation + "' in definition " + name);
        }

        this.operations.add(operation);
        this.operationsParameters.put(operation, params);
    }

    public List<Class<?>> parameters(String operation) {

        for (PathItem path : api.getPaths().values()) {
            List<Class<?>> found = getOperationParameters(path.getGet(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getPost(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getDelete(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getHead(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getOptions(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getPatch(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getPut(), operation);
            if (found != null) {
                return found;
            }
            found = getOperationParameters(path.getTrace(), operation);
            if (found != null) {
                return found;
            }

        }
        return Collections.emptyList();
    }

    public boolean hasParameter(String operation, String param) {
        return operationsParameters.getOrDefault(operation, Collections.emptyMap()).containsKey(param);
    }

    public boolean hasParameter(String operation, String param, String value) {
        return value.equalsIgnoreCase(operationsParameters.getOrDefault(operation, Collections.emptyMap()).get(param));
    }

    private boolean checkOperationById(Operation op, String operation) {
        if (op != null && operation.equals(op.getOperationId())) {
            return true;
        }

        return false;
    }

    private List<Class<?>> getOperationParameters(Operation op, String operation) {
        if (op != null && operation.equals(op.getOperationId())) {
            if (op.getParameters() != null) {
                List<Class<?>> types = op.getParameters().stream().map(p -> openApiTypeToClass(p.getSchema()))
                        .collect(Collectors.toList());

                return types;
            }
            return Collections.emptyList();
        }

        return null;
    }

    public static OpenAPIMetaData of(String url) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(url);
            OpenAPIParser parser = new OpenAPIParser();
            final OpenAPI openAPI = (is != null
                    ? parser.readContents(new String(IoUtils.readBytesFromInputStream(is), StandardCharsets.UTF_8), null,
                            new ParseOptions())
                    : parser.readLocation(url, null, new ParseOptions())).getOpenAPI();

            final String name = openAPI.getInfo().getTitle().replaceAll(" ", "");

            return cached.computeIfAbsent(name, k -> new OpenAPIMetaData(name, openAPI));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Class<?> openApiTypeToClass(Schema<?> type) {
        if ("string".equals(type.getType())) {
            return String.class;
        }
        if ("number".equals(type.getType())) {

            if ("float".equals(type.getFormat())) {
                return Float.class;
            }
            if ("double".equals(type.getFormat())) {
                return Double.class;
            }

            return Integer.class;
        }
        if ("integer".equals(type.getType())) {
            if ("int32".equals(type.getFormat())) {
                return Integer.class;
            }
            if ("int64".equals(type.getFormat())) {
                return Long.class;
            }
            return Integer.class;
        }

        if ("boolean".equals(type.getType())) {
            return Boolean.class;
        }
        if ("array".equals(type.getType())) {
            return List.class;
        }
        if ("object".equals(type.getType())) {
            return Object.class;
        }

        return Object.class;
    }
}
