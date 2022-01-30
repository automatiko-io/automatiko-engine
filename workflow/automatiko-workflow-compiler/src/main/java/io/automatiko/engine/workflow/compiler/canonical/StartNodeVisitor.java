
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_CONDITION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.MESSAGE_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_CORRELATION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_CORRELATION_EXPR;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.factory.MappableNodeFactory.METHOD_OUT_JQ_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.MilestoneNodeFactory.METHOD_CONDITION;
import static io.automatiko.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_INTERRUPTING;
import static io.automatiko.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_TIMER;
import static io.automatiko.engine.workflow.process.executable.core.factory.StartNodeFactory.METHOD_TRIGGER;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskOutputJqAssignmentAction;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.ConstraintTrigger;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.factory.StartNodeFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

public class StartNodeVisitor extends AbstractNodeVisitor<StartNode> {

    @Override
    protected String getNodeKey() {
        return "startNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, StartNode node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, StartNodeFactory.class, getNodeId(node), getNodeKey(),
                new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Start"))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_INTERRUPTING,
                        new BooleanLiteralExpr(node.isInterrupting())));

        visitMetaData(node.getMetaData(), body, getNodeId(node));

        boolean serverless = ProcessToExecModelGenerator.isServerlessWorkflow(process);

        if (serverless) {

            for (DataAssociation association : node.getOutAssociations()) {

                if (association.getAssignments() != null && !association.getAssignments().isEmpty()) {
                    TaskOutputJqAssignmentAction action = (TaskOutputJqAssignmentAction) association.getAssignments().get(0)
                            .getMetaData("Action");

                    String outputFilter = action.getOutputFilterExpression();
                    String scopeFilter = action.getScopeFilter();
                    body.addStatement(
                            getFactoryMethod(getNodeId(node), METHOD_OUT_JQ_MAPPING,
                                    (outputFilter != null ? new StringLiteralExpr().setString(outputFilter)
                                            : new NullLiteralExpr()),
                                    (scopeFilter != null ? new StringLiteralExpr().setString(scopeFilter)
                                            : new NullLiteralExpr()),
                                    new BooleanLiteralExpr(action.isIgnoreScopeFilter())));
                }
            }
        } else {

            for (DataAssociation entry : node.getOutAssociations()) {

                if (entry.getAssignments() != null && !entry.getAssignments().isEmpty()) {
                    Assignment assignment = entry.getAssignments().get(0);
                    body.addStatement(getFactoryMethod(getNodeId(node), "outMapping",
                            new StringLiteralExpr(entry.getSources().get(0)), new NullLiteralExpr(),
                            new StringLiteralExpr(assignment.getDialect()), new StringLiteralExpr(assignment.getFrom()),
                            new StringLiteralExpr(assignment.getTo())));
                } else {
                    body.addStatement(getFactoryMethod(getNodeId(node), "outMapping",
                            new StringLiteralExpr(entry.getSources().get(0)), new StringLiteralExpr(entry.getTarget()),
                            new NullLiteralExpr(), new NullLiteralExpr(), new NullLiteralExpr()));
                }
            }
        }

        body.addStatement(getDoneMethod(getNodeId(node)));
        if (node.getTimer() != null) {
            Timer timer = node.getTimer();
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TIMER, getOrNullExpr(timer.getDelay()),
                    getOrNullExpr(timer.getPeriod()), getOrNullExpr(timer.getDate()),
                    new IntegerLiteralExpr(node.getTimer().getTimeType())));

        } else if (node.getTriggers() != null && !node.getTriggers().isEmpty()) {
            Map<String, Object> nodeMetaData = node.getMetaData();
            TriggerMetaData trigger = new TriggerMetaData((String) nodeMetaData.get(TRIGGER_REF),
                    (String) nodeMetaData.get(TRIGGER_TYPE),
                    (String) nodeMetaData.get(MESSAGE_TYPE), (String) nodeMetaData.get(TRIGGER_MAPPING),
                    String.valueOf(node.getId()), node.getName(), (String) nodeMetaData.get(TRIGGER_CORRELATION),
                    (String) nodeMetaData.get(TRIGGER_CORRELATION_EXPR)).validate();
            trigger.addContext(node.getMetaData());
            trigger.addContext(Collections.singletonMap("_node_", node));
            // mark the trigger as capable of starting new instance only if this is top level start node
            if (node.getParentContainer() instanceof WorkflowProcess) {
                trigger.setStart(true);
            }
            metadata.addTrigger(trigger);

            handleTrigger(node, nodeMetaData, body, variableScope, metadata);

        } else {
            // since there is start node without trigger then make sure it is startable
            metadata.setStartable(true);
        }

    }

    protected void handleTrigger(StartNode startNode, Map<String, Object> nodeMetaData, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        if (EVENT_TYPE_SIGNAL.equalsIgnoreCase((String) startNode.getMetaData(TRIGGER_TYPE))) {
            Variable variable = null;
            Map<String, String> variableMapping = startNode.getOutMappings();
            String variableType = null;
            if (variableMapping != null && !variableMapping.isEmpty()) {
                Entry<String, String> varInfo = variableMapping.entrySet().iterator().next();

                body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                        new StringLiteralExpr((String) nodeMetaData.get(MESSAGE_TYPE)), getOrNullExpr(varInfo.getKey()),
                        getOrNullExpr(varInfo.getValue())));

                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(varInfo.getValue());
                if (matcher.find()) {
                    variableType = (String) nodeMetaData.get(TRIGGER_REF);
                } else {
                    variable = variableScope.findVariable(varInfo.getValue());

                    if (variable == null) {
                        // check parent node container
                        VariableScope vscope = (VariableScope) startNode.resolveContext(VariableScope.VARIABLE_SCOPE,
                                varInfo.getKey());
                        variable = vscope.findVariable(varInfo.getValue());
                    }
                    variableType = variable != null ? variable.getType().getStringType() : null;
                }
            } else {
                body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                        new StringLiteralExpr((String) nodeMetaData.get(MESSAGE_TYPE)),
                        new StringLiteralExpr(getOrDefault((String) nodeMetaData.get(TRIGGER_MAPPING), ""))));
            }
            metadata.addSignal((String) nodeMetaData.get(MESSAGE_TYPE), variableType, startNode);
        } else if (EVENT_TYPE_CONDITION.equalsIgnoreCase((String) startNode.getMetaData(TRIGGER_TYPE))) {
            ConstraintTrigger constraintTrigger = (ConstraintTrigger) startNode.getTriggers().get(0);
            body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_CONDITION,
                    createLambdaExpr(constraintTrigger.getConstraint(), variableScope)));
        } else {
            String triggerMapping = (String) nodeMetaData.get(TRIGGER_MAPPING);
            body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                    new StringLiteralExpr((String) nodeMetaData.get(TRIGGER_REF)),
                    new StringLiteralExpr(getOrDefault((String) nodeMetaData.get(TRIGGER_MAPPING), "")),
                    new StringLiteralExpr(getOrDefault(startNode.getOutMapping(triggerMapping), ""))));
        }
    }
}
