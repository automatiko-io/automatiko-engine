
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatik.engine.workflow.process.executable.core.Metadata.REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;
import static io.automatik.engine.workflow.process.executable.core.factory.EndNodeFactory.METHOD_ACTION;
import static io.automatik.engine.workflow.process.executable.core.factory.EndNodeFactory.METHOD_TERMINATE;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.executable.core.factory.EndNodeFactory;

public class EndNodeVisitor extends AbstractNodeVisitor<EndNode> {

    @Override
    protected String getNodeKey() {
        return "endNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, EndNode node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, EndNodeFactory.class, getNodeId(node), getNodeKey(),
                new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "End"))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_TERMINATE,
                        new BooleanLiteralExpr(node.isTerminate())));

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
        } else if (node.getMetaData(REF) != null && EVENT_TYPE_SIGNAL.equals(node.getMetaData(EVENT_TYPE))) {
            MethodCallExpr getProcessInstance = getFactoryMethod(KCONTEXT_VAR, "getProcessInstance");
            MethodCallExpr signalEventMethod = new MethodCallExpr(getProcessInstance, "signalEvent")
                    .addArgument(new StringLiteralExpr((String) node.getMetaData(REF)))
                    .addArgument(new NullLiteralExpr());
            BlockStmt actionBody = new BlockStmt();
            actionBody.addStatement(signalEventMethod);
            LambdaExpr lambda = new LambdaExpr(new Parameter(new UnknownType(), KCONTEXT_VAR), actionBody);
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_ACTION, lambda));
        }

        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
    }
}
