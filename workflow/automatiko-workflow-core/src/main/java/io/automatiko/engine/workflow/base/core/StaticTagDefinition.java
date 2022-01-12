package io.automatiko.engine.workflow.base.core;

import java.util.Map;

import io.automatiko.engine.api.runtime.process.ProcessInstance;

public class StaticTagDefinition extends TagDefinition {

    public StaticTagDefinition(String id, String expression) {
        super(id, expression);
    }

    @Override
    public String get(ProcessInstance instance, Map<String, Object> variables) {
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
