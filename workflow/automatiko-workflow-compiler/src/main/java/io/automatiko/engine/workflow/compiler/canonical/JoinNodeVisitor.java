
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.factory.JoinFactory.METHOD_NUM_COMPLETED;
import static io.automatiko.engine.workflow.process.executable.core.factory.JoinFactory.METHOD_TYPE;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.executable.core.factory.JoinFactory;

public class JoinNodeVisitor extends AbstractNodeVisitor<Join> {

    @Override
    protected String getNodeKey() {
        return "joinNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, Join node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, JoinFactory.class, getNodeId(node), getNodeKey(),
                new LongLiteralExpr(node.getId())));
        body.addStatement(getNameMethod(node, "Join"));
        body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TYPE, new IntegerLiteralExpr(node.getType())));

        if (node.getType() == Join.TYPE_N_OF_M) {
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_NUM_COMPLETED, new StringLiteralExpr(node.getN())));
        }

        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
    }
}
