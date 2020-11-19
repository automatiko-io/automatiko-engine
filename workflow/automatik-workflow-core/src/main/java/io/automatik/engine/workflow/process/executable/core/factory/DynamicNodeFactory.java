
package io.automatik.engine.workflow.process.executable.core.factory;

import java.util.function.Predicate;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.DynamicNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class DynamicNodeFactory extends CompositeContextNodeFactory {

	public static final String METHOD_LANGUAGE = "language";
	public static final String METHOD_ACTIVATION_EXPRESSION = "activationExpression";
	public static final String METHOD_COMPLETION_EXPRESSION = "completionExpression";

	public DynamicNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	@Override
	protected CompositeContextNode createNode() {
		return new DynamicNode();
	}

	protected DynamicNode getDynamicNode() {
		return (DynamicNode) getNodeContainer();
	}

	@Override
	protected CompositeContextNode getCompositeNode() {
		return (CompositeContextNode) getNodeContainer();
	}

	@Override
	public DynamicNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public DynamicNodeFactory variable(String name, DataType type) {
		super.variable(name, type);
		return this;
	}

	@Override
	public DynamicNodeFactory variable(String name, DataType type, Object value) {
		super.variable(name, type, value);
		return this;
	}

	@Override
	public DynamicNodeFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
		super.exceptionHandler(exception, exceptionHandler);
		return this;
	}

	@Override
	public DynamicNodeFactory exceptionHandler(String exception, String dialect, String action) {
		super.exceptionHandler(exception, dialect, action);
		return this;
	}

	@Override
	public DynamicNodeFactory autoComplete(boolean autoComplete) {
		super.autoComplete(autoComplete);
		return this;
	}

	@Override
	public DynamicNodeFactory linkIncomingConnections(long nodeId) {
		super.linkIncomingConnections(nodeId);
		return this;
	}

	@Override
	public DynamicNodeFactory linkOutgoingConnections(long nodeId) {
		super.linkOutgoingConnections(nodeId);
		return this;
	}

	@Override
	public DynamicNodeFactory metaData(String name, Object value) {
		super.metaData(name, value);
		return this;
	}

	public DynamicNodeFactory language(String language) {
		getDynamicNode().setLanguage(language);
		return this;
	}

	public DynamicNodeFactory activationExpression(Predicate<ProcessContext> activationExpression) {
		getDynamicNode().setActivationExpression(activationExpression);
		return this;
	}

	public DynamicNodeFactory completionExpression(Predicate<ProcessContext> completionExpression) {
		getDynamicNode().setCompletionExpression(completionExpression);
		return this;
	}
}
