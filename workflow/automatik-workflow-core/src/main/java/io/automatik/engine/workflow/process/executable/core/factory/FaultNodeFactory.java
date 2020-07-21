
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.FaultNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class FaultNodeFactory extends ExtendedNodeFactory {

	public static final String METHOD_FAULT_NAME = "faultName";
	public static final String METHOD_FAULT_VARIABLE = "faultVariable";

	public FaultNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new FaultNode();
	}

	protected FaultNode getFaultNode() {
		return (FaultNode) getNode();
	}

	@Override
	public FaultNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	public FaultNodeFactory faultVariable(String faultVariable) {
		getFaultNode().setFaultVariable(faultVariable);
		return this;
	}

	public FaultNodeFactory faultName(String faultName) {
		getFaultNode().setFaultName(faultName);
		return this;
	}
}
