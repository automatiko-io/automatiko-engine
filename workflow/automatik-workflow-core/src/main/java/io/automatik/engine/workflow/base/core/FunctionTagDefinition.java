package io.automatik.engine.workflow.base.core;

import java.util.Map;
import java.util.function.BiFunction;

public class FunctionTagDefinition extends TagDefinition {

    private BiFunction<String, Map<String, Object>, String> function;

    public FunctionTagDefinition(String id, String expression,
            BiFunction<String, Map<String, Object>, String> function) {
        super(id, expression);
        this.function = function;
    }

    @Override
    public String get(Map<String, Object> variables) {

        return function.apply(expression, variables);
    }

    @Override
    public String getType() {
        return "function";
    }

    @Override
    public String toString() {
        return "FunctionTagDefinition [id=" + id + ", expression=" + expression + "]";
    }

}
