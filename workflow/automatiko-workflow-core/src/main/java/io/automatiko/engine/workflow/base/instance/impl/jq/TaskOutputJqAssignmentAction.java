package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class TaskOutputJqAssignmentAction implements AssignmentAction {

    private String outputFilterExpression;
    private String scopeFilter;
    private boolean ignoreScopeFilter;

    public TaskOutputJqAssignmentAction(String output, String scope) {
        this.outputFilterExpression = output;
        this.scopeFilter = scope == null ? output : scope;
    }

    public TaskOutputJqAssignmentAction(String output, String scope, boolean ignoreScopeFilter) {
        this.outputFilterExpression = output;
        this.ignoreScopeFilter = ignoreScopeFilter;
        if (ignoreScopeFilter) {
            this.scopeFilter = scope;
        } else {
            this.scopeFilter = scope == null ? output : scope;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void execute(WorkItem workItem, ProcessContext context) throws Exception {
        JsonNode sdata = (JsonNode) workItem.getResult(JsonVariableScope.WORKFLOWDATA_KEY);
        if (outputFilterExpression != null) {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                    .getProcess())
                            .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);

            Map<String, Object> vars = new HashMap<>();
            vars.put("workflowdata", sdata);

            Map<String, Object> variables = new HashMap<>();
            if (context.getVariable("$CONST") != null) {

                variables.put("CONST", context.getVariable("$CONST"));
            }

            vars.put("workflow_variables", variables);
            sdata = (JsonNode) evaluator.evaluate(outputFilterExpression, vars);

            if (scopeFilter != null) {
                variables.put("v", sdata);
                sdata = (JsonNode) evaluator.evaluate(scopeFilter + "=$v", vars);
            }

        }

        ObjectMapper mapper = new ObjectMapper();

        if (sdata != null) {
            Object updated = mapper.readerForUpdating(context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY))
                    .readValue(sdata);

            context.setVariable(JsonVariableScope.WORKFLOWDATA_KEY, updated);
        }

    }

    public String getOutputFilterExpression() {
        return outputFilterExpression;
    }

    public String getScopeFilter() {
        return scopeFilter;
    }

    public boolean isIgnoreScopeFilter() {
        return ignoreScopeFilter;
    }

}
