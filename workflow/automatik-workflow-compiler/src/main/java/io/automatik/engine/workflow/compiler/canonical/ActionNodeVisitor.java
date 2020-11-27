
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatik.engine.workflow.process.executable.core.factory.ActionNodeFactory.METHOD_ACTION;

import java.util.List;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.executable.core.factory.ActionNodeFactory;

public class ActionNodeVisitor extends AbstractNodeVisitor<ActionNode> {

    @Override
    protected String getNodeKey() {
        return "actionNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, ActionNode node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, ActionNodeFactory.class, getNodeId(node), getNodeKey(),
                new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Script"));

        // if there is trigger defined on end event create TriggerMetaData for it
        if (node.getMetaData(TRIGGER_REF) != null) {
            LambdaExpr lambda = TriggerMetaData.buildLambdaExpr(node, metadata);
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_ACTION, lambda));
        } else if (node.getMetaData(TRIGGER_TYPE) != null && node.getMetaData(TRIGGER_TYPE).equals("Compensation")) {
            // compensation

            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_ACTION,
                    new ObjectCreationExpr(null, new ClassOrInterfaceType(null,
                            "io.automatik.engine.workflow.base.instance.impl.actions.ProcessInstanceCompensationAction"),
                            NodeList.nodeList(new StringLiteralExpr((String) node.getMetaData("CompensationEvent"))))));
        } else {
            String consequence = getActionConsequence(node.getAction());
            if (consequence == null || consequence.trim().isEmpty()) {
                throw new IllegalStateException(
                        "Action node " + node.getId() + " name " + node.getName() + " has no action defined");
            }
            BlockStmt actionBody = new BlockStmt();
            List<Variable> variables = variableScope.getVariables();
            variables.stream().filter(v -> consequence.contains(v.getName())).map(ActionNodeVisitor::makeAssignment)
                    .forEach(actionBody::addStatement);

            actionBody.addStatement(new NameExpr(consequence));

            LambdaExpr lambda = new LambdaExpr(new Parameter(new UnknownType(), KCONTEXT_VAR), // (kcontext) ->
                    actionBody);
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_ACTION, lambda));
        }
        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
    }

    private String getActionConsequence(ProcessAction action) {
        if (!(action instanceof ConsequenceAction)) {
            return null;
        }
        return ((ConsequenceAction) action).getConsequence();
    }
}
