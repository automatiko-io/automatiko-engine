package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.ContextableInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class InputJqAssignmentAction implements AssignmentAction {

    private String inputFilterExpression;

    public InputJqAssignmentAction(String input) {
        this.inputFilterExpression = input;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void execute(WorkItem workItem, ProcessContext context) throws Exception {
        Object sdata = context.getProcessInstance().getVariable(JsonVariableScope.WORKFLOWDATA_KEY);
        if (inputFilterExpression != null) {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                    .getProcess())
                            .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);

            Map<String, Object> vars = new HashMap<>();
            vars.put("workflowdata", sdata);
            if (context.getVariable("$CONST") != null) {
                vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
            }
            sdata = evaluator.evaluate(inputFilterExpression, vars);
        }
        if (context.getNodeInstance() instanceof ContextableInstance) {
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ContextableInstance) context
                    .getNodeInstance()).getContextInstance(VariableScope.VARIABLE_SCOPE);

            variableScopeInstance.setVariable(JsonVariableScope.WORKFLOWDATA_KEY, sdata);
        }
    }

    public String getInputFilterExpression() {
        return inputFilterExpression;
    }

}
