
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.MilestoneNodeFactory.METHOD_CONDITION;
import static io.automatik.engine.workflow.process.executable.core.factory.StateNodeFactory.METHOD_CONSTRAINT;

import java.util.Map;
import java.util.stream.Stream;

import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.utils.StringEscapeUtils;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.StateNodeFactory;

public class StateNodeVisitor extends CompositeContextNodeVisitor<StateNode> {

    public StateNodeVisitor(Map<Class<?>, AbstractNodeVisitor<? extends Node>> nodesVisitors) {
        super(nodesVisitors);
    }

    @Override
    protected Class<? extends CompositeContextNodeFactory> factoryClass() {
        return StateNodeFactory.class;
    }

    @Override
    protected String getNodeKey() {
        return "stateNode";
    }

    @Override
    protected String getDefaultName() {
        return "State";
    }

    @Override
    public Stream<MethodCallExpr> visitCustomFields(StateNode node, VariableScope variableScope) {
        if (node.getMetaData("Condition") != null) {
            String condition = (String) node.getMetaData("Condition");

            return Stream.of(getFactoryMethod(getNodeId(node), METHOD_CONDITION, createLambdaExpr(condition, variableScope)));
        } else if (node.getConstraints() != null) {

            return node.getConstraints().entrySet().stream()
                    .map((e -> getFactoryMethod(getNodeId(node), METHOD_CONSTRAINT,
                            getOrNullExpr(e.getKey().getConnectionId()), new LongLiteralExpr(e.getKey().getNodeId()),
                            new StringLiteralExpr(e.getKey().getToType()), new StringLiteralExpr(e.getValue().getDialect()),
                            new StringLiteralExpr(StringEscapeUtils.escapeJava(e.getValue().getConstraint())),
                            new IntegerLiteralExpr(e.getValue().getPriority()))));
        }

        return Stream.empty();
    }
}
