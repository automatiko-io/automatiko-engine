
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.process.executable.core.factory.DynamicNodeFactory.METHOD_ACTIVATION_EXPRESSION;
import static io.automatik.engine.workflow.process.executable.core.factory.DynamicNodeFactory.METHOD_COMPLETION_EXPRESSION;
import static io.automatik.engine.workflow.process.executable.core.factory.DynamicNodeFactory.METHOD_LANGUAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import com.github.javaparser.ast.expr.MethodCallExpr;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.node.DynamicNode;
import io.automatik.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory;
import io.automatik.engine.workflow.process.executable.core.factory.DynamicNodeFactory;

public class DynamicNodeVisitor extends CompositeContextNodeVisitor<DynamicNode> {

	public DynamicNodeVisitor(Map<Class<?>, AbstractNodeVisitor<? extends Node>> nodesVisitors) {
		super(nodesVisitors);
	}

	@Override
	protected Class<? extends CompositeContextNodeFactory> factoryClass() {
		return DynamicNodeFactory.class;
	}

	@Override
	protected String factoryMethod() {
		return "dynamicNode";
	}

	@Override
	protected String getDefaultName() {
		return "Dynamic";
	}

	@Override
	public Stream<MethodCallExpr> visitCustomFields(DynamicNode node, VariableScope variableScope) {
		Collection<MethodCallExpr> methods = new ArrayList<>();
		methods.add(getFactoryMethod(getNodeId(node), METHOD_LANGUAGE, getOrNullExpr(node.getLanguage())));
		if (node.getActivationCondition() != null && !node.getActivationCondition().trim().isEmpty()) {
			methods.add(getActivationConditionStatement(node, variableScope));
		}
		if (node.getCompletionCondition() != null && !node.getCompletionCondition().trim().isEmpty()) {
			methods.add(getCompletionConditionStatement(node, variableScope));
		}
		return methods.stream();
	}

	private MethodCallExpr getActivationConditionStatement(DynamicNode node, VariableScope scope) {
		return getFactoryMethod(getNodeId(node), METHOD_ACTIVATION_EXPRESSION,
				createLambdaExpr(node.getActivationCondition(), scope));
	}

	private MethodCallExpr getCompletionConditionStatement(DynamicNode node, VariableScope scope) {
		return getFactoryMethod(getNodeId(node), METHOD_COMPLETION_EXPRESSION,
				createLambdaExpr(node.getCompletionCondition(), scope));
	}
}
