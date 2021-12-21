package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.ProcessContext;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;

public class OutputJqAssignmentAction implements AssignmentAction {

    private String outputFilterExpression;

    public OutputJqAssignmentAction(String output) {
        this.outputFilterExpression = output;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void execute(WorkItem workItem, ProcessContext context) throws Exception {
        Object sdata = context.getVariable(JsonVariableScope.WORKFLOWDATA_KEY);
        if (outputFilterExpression != null) {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) context.getProcessInstance()
                    .getProcess())
                            .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);

            Map<String, Object> vars = new HashMap<>();
            vars.put("workflowdata", sdata);
            if (context.getVariable("$CONST") != null) {
                vars.put("workflow_variables", Collections.singletonMap("CONST", context.getVariable("$CONST")));
            }
            sdata = evaluator.evaluate(outputFilterExpression, vars);
        }

        ObjectNode workflowData = (ObjectNode) context.getProcessInstance().getVariable(JsonVariableScope.WORKFLOWDATA_KEY);
        ObjectMapper mapper = new ObjectMapper();

        Object updated = mapper.readerForUpdating(workflowData).readValue((JsonNode) sdata);
        context.getProcessInstance().setVariable(JsonVariableScope.WORKFLOWDATA_KEY, updated);
    }

}
