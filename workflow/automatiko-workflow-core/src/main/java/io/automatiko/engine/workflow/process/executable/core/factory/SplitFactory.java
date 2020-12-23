
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.workflow.base.instance.impl.ReturnValueConstraintEvaluator;
import io.automatiko.engine.workflow.base.instance.impl.ReturnValueEvaluator;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;
import io.automatiko.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class SplitFactory extends NodeFactory {

	public static final String METHOD_TYPE = "type";
	public static final String METHOD_CONSTRAINT = "constraint";

	public SplitFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new Split();
	}

	protected Split getSplit() {
		return (Split) getNode();
	}

	@Override
	public SplitFactory name(String name) {
		super.name(name);
		return this;
	}

	public SplitFactory type(int type) {
		getSplit().setType(type);
		return this;
	}

	public SplitFactory constraint(long toNodeId, String name, String type, String dialect, String constraint) {
		return constraint(toNodeId, name, type, dialect, constraint, 0);
	}

	public SplitFactory constraint(long toNodeId, String name, String type, String dialect, String constraint,
			int priority) {
		ConstraintImpl constraintImpl = new ConstraintImpl();
		constraintImpl.setName(name);
		constraintImpl.setType(type);
		constraintImpl.setDialect(dialect);
		constraintImpl.setConstraint(constraint);
		constraintImpl.setPriority(priority);
		getSplit().addConstraint(new ConnectionRef(toNodeId, Node.CONNECTION_DEFAULT_TYPE), constraintImpl);
		return this;
	}

	public SplitFactory constraint(long toNodeId, String name, String type, String dialect,
			ReturnValueEvaluator evaluator, int priority) {
		ReturnValueConstraintEvaluator constraintImpl = new ReturnValueConstraintEvaluator();
		constraintImpl.setName(name);
		constraintImpl.setType(type);
		constraintImpl.setDialect(dialect);
		constraintImpl.setPriority(priority);
		constraintImpl.setEvaluator(evaluator);
		constraintImpl.setConstraint("expression already given as evaluator");
		getSplit().addConstraint(new ConnectionRef(name, toNodeId, Node.CONNECTION_DEFAULT_TYPE), constraintImpl);
		return this;
	}
}
