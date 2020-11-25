
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.MilestoneNodeFactory.METHOD_CONDITION;

import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.MilestoneNode;
import io.automatik.engine.workflow.process.executable.core.factory.MilestoneNodeFactory;

public class MilestoneNodeVisitor extends AbstractNodeVisitor<MilestoneNode> {

    @Override
    protected String getNodeKey() {
        return "milestoneNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, MilestoneNode node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, MilestoneNodeFactory.class, getNodeId(node),
                getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Milestone"));
        if (node.getConditionExpression() != null && !node.getConditionExpression().trim().isEmpty()) {
            body.addStatement(getConditionStatement(node, variableScope));
        }
        body.addStatement(getDoneMethod(getNodeId(node)));
        visitMetaData(node.getMetaData(), body, getNodeId(node));
    }

    private MethodCallExpr getConditionStatement(MilestoneNode node, VariableScope scope) {
        return getFactoryMethod(getNodeId(node), METHOD_CONDITION, createLambdaExpr(node.getConditionExpression(), scope));
    }

}
