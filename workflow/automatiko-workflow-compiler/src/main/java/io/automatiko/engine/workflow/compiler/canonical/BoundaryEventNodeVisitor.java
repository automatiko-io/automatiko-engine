
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_CONDITION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_MESSAGE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.MESSAGE_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.factory.BoundaryEventNodeFactory.METHOD_ATTACHED_TO;
import static io.automatiko.engine.workflow.process.executable.core.factory.EventNodeFactory.METHOD_EVENT_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.factory.EventNodeFactory.METHOD_SCOPE;
import static io.automatiko.engine.workflow.process.executable.core.factory.EventNodeFactory.METHOD_VARIABLE_NAME;
import static io.automatiko.engine.workflow.process.executable.core.factory.MilestoneNodeFactory.METHOD_CONDITION;

import java.util.Collections;
import java.util.Map;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.executable.core.factory.BoundaryEventNodeFactory;

public class BoundaryEventNodeVisitor extends AbstractNodeVisitor<BoundaryEventNode> {

    @Override
    protected String getNodeKey() {
        return "boundaryEventNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, BoundaryEventNode node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, BoundaryEventNodeFactory.class, getNodeId(node),
                getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "BoundaryEvent"))
                .addStatement(
                        getFactoryMethod(getNodeId(node), METHOD_EVENT_TYPE, new StringLiteralExpr(node.getType())))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_ATTACHED_TO,
                        new StringLiteralExpr(node.getAttachedToNodeId())))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_SCOPE, getOrNullExpr(node.getScope())));

        Variable variable = null;
        if (node.getVariableName() != null) {
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_VARIABLE_NAME,
                    new StringLiteralExpr(node.getVariableName())));
            variable = variableScope.findVariable(node.getVariableName());
        }

        if (EVENT_TYPE_SIGNAL.equals(node.getMetaData(EVENT_TYPE))) {
            metadata.addSignal(node.getType(), variable != null ? variable.getType().getStringType() : null, node);
        } else if (EVENT_TYPE_MESSAGE.equals(node.getMetaData(EVENT_TYPE))) {
            Map<String, Object> nodeMetaData = node.getMetaData();
            TriggerMetaData triggerMetaData = new TriggerMetaData((String) nodeMetaData.get(TRIGGER_REF),
                    (String) nodeMetaData.get(TRIGGER_TYPE), (String) nodeMetaData.get(MESSAGE_TYPE),
                    node.getVariableName(), String.valueOf(node.getId()), node.getName()).validate();
            triggerMetaData.addContext(node.getMetaData());
            triggerMetaData.addContext(Collections.singletonMap("_node_", node));
            metadata.addTrigger(triggerMetaData);
        } else if (EVENT_TYPE_CONDITION.equalsIgnoreCase((String) node.getMetaData(EVENT_TYPE))) {
            String condition = (String) node.getMetaData("Condition");
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_CONDITION,
                    createLambdaExpr(condition, variableScope)));
        }

        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
    }
}
