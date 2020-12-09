
package io.automatik.engine.workflow.base.core.datatype.impl.coverter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypeConverterRegistry {

    private static TypeConverterRegistry INSTANCE = new TypeConverterRegistry();

    private Map<String, Function<String, ? extends Object>> converters = new HashMap<>();
    private Function<String, String> defaultConverter = new NoOpTypeConverter();

    private TypeConverterRegistry() {
        converters.put("java.util.Date", new DateTypeConverter());
        converters.put("java.lang.Double", s -> Double.valueOf(s));
        converters.put("java.math.BigDecimal", s -> BigDecimal.valueOf(Double.valueOf(s)));
    }

    public Function<String, ? extends Object> forType(String type) {
        return converters.getOrDefault(type, defaultConverter);
    }

    public void register(String type, Function<String, ? extends Object> converter) {
        this.converters.putIfAbsent(type, converter);
    }

    public static TypeConverterRegistry get() {
        return INSTANCE;
    }
}
