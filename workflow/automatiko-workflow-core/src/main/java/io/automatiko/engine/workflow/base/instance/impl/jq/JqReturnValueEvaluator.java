package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.BooleanNode;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.instance.impl.ReturnValueEvaluator;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class JqReturnValueEvaluator implements ReturnValueEvaluator {

    private final String expression;

    public JqReturnValueEvaluator(String expression) {
        this.expression = expression;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object evaluate(ProcessContext context) throws Exception {
        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
        Map<String, Object> vars = new HashMap<>();
        vars.put("workflowdata", context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY));
        if (context.getVariable("$CONST") != null) {
            vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
        }
        Object content = evaluator.evaluate(expression, vars);

        if (content == null) {
            return false;
        }

        if (content instanceof BooleanNode) {
            return ((BooleanNode) content).asBoolean();
        }

        return false;
    }

}
