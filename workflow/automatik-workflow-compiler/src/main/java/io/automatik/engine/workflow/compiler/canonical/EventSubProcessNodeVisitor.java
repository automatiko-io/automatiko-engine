
package io.automatik.engine.workflow.compiler.canonical;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.EventSubProcessNodeFactory;

import static io.automatik.engine.workflow.process.executable.core.factory.EventSubProcessNodeFactory.METHOD_EVENT;
import static io.automatik.engine.workflow.process.executable.core.factory.EventSubProcessNodeFactory.METHOD_KEEP_ACTIVE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class EventSubProcessNodeVisitor extends CompositeContextNodeVisitor<EventSubProcessNode> {

	public EventSubProcessNodeVisitor(Map<Class<?>, AbstractNodeVisitor<? extends Node>> nodesVisitors) {
		super(nodesVisitors);
	}

	@Override
	protected Class<? extends CompositeContextNodeFactory> factoryClass() {
		return EventSubProcessNodeFactory.class;
	}

	@Override
	protected String getNodeKey() {
		return "eventSubProcessNode";
	}

	@Override
	protected String getDefaultName() {
		return "EventSubProcess";
	}

	@Override
	public Stream<MethodCallExpr> visitCustomFields(EventSubProcessNode node, VariableScope variableScope) {
		Collection<MethodCallExpr> methods = new ArrayList<>();
		methods.add(getFactoryMethod(getNodeId(node), METHOD_KEEP_ACTIVE, new BooleanLiteralExpr(node.isKeepActive())));
		node.getEvents()
				.forEach(e -> methods.add(getFactoryMethod(getNodeId(node), METHOD_EVENT, new StringLiteralExpr(e))));
		return methods.stream();
	}
}
