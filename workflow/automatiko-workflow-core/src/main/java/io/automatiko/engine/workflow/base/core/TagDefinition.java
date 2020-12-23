package io.automatiko.engine.workflow.base.core;

import java.util.Map;

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

    public abstract String get(Map<String, Object> variables);

}
