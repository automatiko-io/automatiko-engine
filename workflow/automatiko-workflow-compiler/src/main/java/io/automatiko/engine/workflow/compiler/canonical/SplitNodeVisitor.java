
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.factory.SplitFactory.METHOD_CONSTRAINT;
import static io.automatiko.engine.workflow.process.executable.core.factory.SplitFactory.METHOD_TYPE;

import java.util.Map.Entry;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.executable.core.factory.SplitFactory;

public class SplitNodeVisitor extends AbstractNodeVisitor<Split> {

    @Override
    protected String getNodeKey() {
        return "splitNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, Split node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, SplitFactory.class, getNodeId(node), getNodeKey(),
                new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Split"))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_TYPE, new IntegerLiteralExpr(node.getType())));

        visitMetaData(node.getMetaData(), body, getNodeId(node));

        if (node.getType() == Split.TYPE_OR || node.getType() == Split.TYPE_XOR) {
            for (Entry<ConnectionRef, Constraint> entry : node.getConstraints().entrySet()) {
                if (entry.getValue() != null) {
                    BlockStmt actionBody = new BlockStmt();
                    LambdaExpr lambda = new LambdaExpr(new Parameter(new UnknownType(), KCONTEXT_VAR), // (kcontext) ->
                            actionBody);

                    for (Variable v : variableScope.getVariables()) {
                        actionBody.addStatement(makeAssignment(v));
                    }

                    if (entry.getValue().getConstraint().contains(System.getProperty("line.separator"))) {
                        BlockStmt constraintBody = new BlockStmt();
                        constraintBody.addStatement(entry.getValue().getConstraint());

                        actionBody.addStatement(constraintBody);
                    } else {
                        actionBody
                                .addStatement(new ReturnStmt(new EnclosedExpr(new NameExpr(entry.getValue().getConstraint()))));
                    }

                    body.addStatement(getFactoryMethod(getNodeId(node), METHOD_CONSTRAINT,
                            new LongLiteralExpr(entry.getKey().getNodeId()),
                            new StringLiteralExpr(getOrDefault(entry.getKey().getConnectionId(), "")),
                            new StringLiteralExpr(entry.getKey().getToType()),
                            new StringLiteralExpr(entry.getValue().getDialect()), lambda,
                            new IntegerLiteralExpr(entry.getValue().getPriority())));
                }
            }
        }
        body.addStatement(getDoneMethod(getNodeId(node)));
    }
}
