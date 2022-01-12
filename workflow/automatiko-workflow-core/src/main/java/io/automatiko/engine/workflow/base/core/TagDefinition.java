package io.automatiko.engine.workflow.base.core;

import java.util.Map;

import io.automatiko.engine.api.runtime.process.ProcessInstance;

public abstract class TagDefinition {

    protected final String id;

    protected final String expression;

    public TagDefinition(String id, String expression) {
        this.id = id;
        this.expression = expression;
    }

    public String getId() {
        return id;
    }

    public abstract String getType();

    public String getExpression() {
        return expression;
    }

    public abstract String get(ProcessInstance instance, Map<String, Object> variables);

}
