
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class JoinFactory extends NodeFactory {

	public static final String METHOD_TYPE = "type";

	public JoinFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new Join();
	}

	protected Join getJoin() {
		return (Join) getNode();
	}

	@Override
	public JoinFactory name(String name) {
		super.name(name);
		return this;
	}

	public JoinFactory type(int type) {
		getJoin().setType(type);
		return this;
	}

	public JoinFactory type(String n) {
		getJoin().setN(n);
		return this;
	}

}
