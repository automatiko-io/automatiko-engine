package io.automatiko.engine.workflow.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuilderContext {

    private static Map<String, List<String>> DATA = new LinkedHashMap<>();

    public static void addMethodData(String methodName, List<String> data) {
        DATA.put(methodName, data);
    }

    public static String get(String methodName) {
        List<String> data = DATA.get(methodName);
        if (data == null) {
            throw new IllegalStateException("Missing lambda expressions for method '" + methodName + "'");
        }
        return data.remove(0);
    }

    public static void clear() {
        DATA.clear();
    }
}
