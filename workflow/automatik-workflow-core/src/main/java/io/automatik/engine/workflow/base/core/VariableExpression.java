package io.automatik.engine.workflow.base.core;

import io.automatik.engine.api.runtime.process.ProcessContext;

public interface VariableExpression {

    <T> T evaluate(String expression, ProcessContext context, Class<T> clazz);

    default String evaluate(String expression, ProcessContext context) {
        return evaluate(expression, context, String.class);
    }
}
