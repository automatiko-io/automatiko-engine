package io.automatiko.engine.workflow.base.core;

import java.util.Map;
import java.util.function.BiFunction;

import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class FunctionTagDefinition extends TagDefinition {

    private BiFunction<String, ProcessContext, String> function;

    public FunctionTagDefinition(String id, String expression,
            BiFunction<String, ProcessContext, String> function) {
        super(id, expression);
        this.function = function;
    }

    @Override
    public String get(ProcessInstance instance, Map<String, Object> variables) {
        try {
            io.automatiko.engine.workflow.base.core.context.ProcessContext ctx = new io.automatiko.engine.workflow.base.core.context.ProcessContext(
                    ((WorkflowProcessInstanceImpl) instance).getProcessRuntime());
            ctx.setProcessInstance(instance);
            Object value = function.apply(expression, ctx);

            if (value == null) {
                return null;
            }

            return value.toString();
        } catch (Exception e) {
            return null;
        }
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
