package io.automatiko.engine.quarkus.variable;

import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;

import io.automatiko.engine.api.Functions;

public class VariableFunctions implements Functions {

    public static <T> T configValue(String name, Class<T> clazz) {
        return ConfigProvider.getConfig().getOptionalValue(name, clazz).orElse(null);
    }

    public static <T> List<T> configValues(String name, Class<T> clazz) {
        return ConfigProvider.getConfig().getOptionalValues(name, clazz).orElse(List.of());
    }

    public static String configValue(String name) {
        return ConfigProvider.getConfig().getOptionalValue(name, String.class).orElse(null);
    }

    public static List<String> configValues(String name) {
        return ConfigProvider.getConfig().getOptionalValues(name, String.class).orElse(List.of());
    }

    public static Integer configIntValue(String name) {
        return ConfigProvider.getConfig().getOptionalValue(name, Integer.class).orElse(null);
    }

    public static List<Integer> configIntValues(String name) {
        return ConfigProvider.getConfig().getOptionalValues(name, Integer.class).orElse(List.of());
    }

    public static Long configLongValue(String name) {
        return ConfigProvider.getConfig().getOptionalValue(name, Long.class).orElse(null);
    }

    public static List<Long> configLongValues(String name) {
        return ConfigProvider.getConfig().getOptionalValues(name, Long.class).orElse(List.of());
    }
}
