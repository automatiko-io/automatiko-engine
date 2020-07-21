
package io.automatik.engine.workflow.process.executable.core.factory;

import static io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE;

import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.impl.ConnectionRef;
import io.automatik.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

/**
 *
 */
public class StateNodeFactory extends CompositeContextNodeFactory {

	public static final String METHOD_CONSTRAINT = "constraint";

	public StateNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	@Override
	protected CompositeContextNode createNode() {
		return new StateNode();
	}

	protected StateNode getStateNode() {
		return (StateNode) getNodeContainer();
	}

	@Override
	protected CompositeContextNode getCompositeNode() {
		return (CompositeContextNode) getNodeContainer();
	}

	@Override
	public StateNodeFactory variable(String name, DataType type) {
		super.variable(name, type);
		return this;
	}

	@Override
	public StateNodeFactory variable(String name, DataType type, Object value) {
		super.variable(name, type, value);
		return this;
	}

	@Override
	public StateNodeFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
		super.exceptionHandler(exception, exceptionHandler);
		return this;
	}

	@Override
	public StateNodeFactory exceptionHandler(String exception, String dialect, String action) {
		super.exceptionHandler(exception, dialect, action);
		return this;
	}

	@Override
	public StateNodeFactory autoComplete(boolean autoComplete) {
		super.autoComplete(autoComplete);
		return this;
	}

	@Override
	public StateNodeFactory linkIncomingConnections(long nodeId) {
		super.linkIncomingConnections(nodeId);
		return this;
	}

	@Override
	public StateNodeFactory linkOutgoingConnections(long nodeId) {
		super.linkOutgoingConnections(nodeId);
		return this;
	}

	public StateNodeFactory constraint(String connectionId, long nodeId, String type, String dialect, String constraint,
			int priority) {
		ConstraintImpl constraintImpl = new ConstraintImpl();
		constraintImpl.setName(connectionId);
		constraintImpl.setType(type);
		constraintImpl.setDialect(dialect);
		constraintImpl.setConstraint(constraint);
		constraintImpl.setPriority(priority);
		getStateNode().addConstraint(new ConnectionRef(connectionId, nodeId, CONNECTION_DEFAULT_TYPE), constraintImpl);
		return this;
	}
}
