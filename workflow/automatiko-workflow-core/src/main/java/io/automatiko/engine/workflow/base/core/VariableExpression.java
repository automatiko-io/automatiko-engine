package io.automatiko.engine.workflow.base.core;

import io.automatiko.engine.api.runtime.process.ProcessContext;

public interface VariableExpression {

    <T> T evaluate(String expression, ProcessContext context, Class<T> clazz);

    default String evaluate(String expression, ProcessContext context) {
        return evaluate(expression, context, String.class);
    }
}
