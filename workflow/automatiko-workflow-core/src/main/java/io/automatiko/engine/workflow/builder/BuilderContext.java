package io.automatiko.engine.workflow.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuilderContext {

    private static Map<String, List<String>> DATA = new LinkedHashMap<>();

    private static Map<String, String> REUSE_DATA = new LinkedHashMap<>();

    public static void addMethodData(String methodName, List<String> data) {
        DATA.put(methodName, data);
    }

    public static String get(String methodName) {
        List<String> data = DATA.get(methodName);
        if (data == null || data.isEmpty()) {
            if (REUSE_DATA.containsKey(methodName)) {
                return REUSE_DATA.get(methodName);
            }
            throw new IllegalStateException("Missing lambda expressions for method '" + methodName + "'");
        }
        String expression = data.remove(0);
        if (data.isEmpty()) {
            // allows to reuse single expression methods - usually private utility methods
            REUSE_DATA.put(methodName, expression);
        }
        return expression;
    }

    public static void clear() {
        DATA.clear();
        REUSE_DATA.clear();
    }
}
