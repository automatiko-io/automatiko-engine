package io.automatik.engine.workflow.base.core;

import java.util.Map;

public class StaticTagDefinition extends TagDefinition {

    public StaticTagDefinition(String id, String expression) {
        super(id, expression);
    }

    @Override
    public String get(Map<String, Object> variables) {
        return expression;
    }

    @Override
    public String getType() {
        return "static";
    }

    @Override
    public String toString() {
        return "StaticTagDefinition [id=" + id + ", value=" + expression + "]";
    }
}
