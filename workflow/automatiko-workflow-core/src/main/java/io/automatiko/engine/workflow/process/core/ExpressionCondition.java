package io.automatiko.engine.workflow.process.core;

import java.util.Map;

public interface ExpressionCondition {

    boolean isValid(Map<String, Object> inputs);
}
