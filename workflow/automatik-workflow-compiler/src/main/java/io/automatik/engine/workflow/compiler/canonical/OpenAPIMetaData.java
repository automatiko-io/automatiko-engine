package io.automatik.engine.workflow.compiler.canonical;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.automatik.engine.services.utils.IoUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIMetaData {

    private static Map<String, OpenAPIMetaData> cached = new ConcurrentHashMap<String, OpenAPIMetaData>();

    private final String name;

    private final OpenAPI api;

    private final Set<String> operations = new LinkedHashSet<String>();

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

    public void addOperation(String operation) {
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
    }

    private boolean checkOperationById(Operation op, String operation) {
        if (op != null && operation.equals(op.getOperationId())) {
            return true;
        }

        return false;
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
}
