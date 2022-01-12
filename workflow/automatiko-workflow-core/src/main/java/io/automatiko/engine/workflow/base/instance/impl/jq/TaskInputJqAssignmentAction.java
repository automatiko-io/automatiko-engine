package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.ContextableInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class TaskInputJqAssignmentAction implements AssignmentAction {

    private String inputFilterExpression;
    private Set<String> paramNames;

    public TaskInputJqAssignmentAction(String input, Set<String> paramNames) {
        this.inputFilterExpression = input;
        this.paramNames = paramNames;
    }

    public TaskInputJqAssignmentAction(String input, String... paramNames) {
        this.inputFilterExpression = input;
        if (paramNames != null && paramNames.length > 0) {
            this.paramNames = new LinkedHashSet<>();

            for (String param : paramNames) {
                this.paramNames.add(param);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void execute(WorkItem workItem, ProcessContext context) throws Exception {
        Object sdata = context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY);

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                .getProcess())
                        .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);

        Map<String, Object> vars = new HashMap<>();
        vars.put("workflowdata", sdata);
        if (context.getVariable("$CONST") != null) {
            vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
        }
        if (inputFilterExpression != null) {
            sdata = evaluator.evaluate(inputFilterExpression, vars);
        }
        if (context.getNodeInstance() instanceof ContextableInstance) {
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((ContextableInstance) context
                    .getNodeInstance()).getContextInstance(VariableScope.VARIABLE_SCOPE);

            variableScopeInstance.setVariable(JsonVariableScope.WORKFLOWDATA_KEY, sdata);
        }

        if (paramNames != null && !paramNames.isEmpty()) {
            vars = new HashMap<>();
            vars.put("workflowdata", sdata);
            if (context.getVariable("$CONST") != null) {
                vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
            }
            for (String name : paramNames) {

                Object param = workItem.getParameter(name);
                if (param != null) {
                    param = evaluator.evaluate(param.toString(), vars);
                    ((WorkItemImpl) workItem).setParameter(name, param);
                }
            }
        } else {
            ((WorkItemImpl) workItem).setParameter("Parameter", sdata);
        }
    }

    public String getInputFilterExpression() {
        return inputFilterExpression;
    }

    public Set<String> getParamNames() {
        return paramNames;
    }

}
