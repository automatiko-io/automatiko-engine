package io.automatiko.engine.api.expression;

import java.util.Collection;
import java.util.Map;

public interface ExpressionEvaluator<T> {

    public static final String EXPRESSION_EVALUATOR = "ExpressionEvaluator";

    Object evaluate(String expression, Map<String, Object> variables);

    Object evaluate(String expression, T resolver);

    void addImports(Collection<String> imports);
}
