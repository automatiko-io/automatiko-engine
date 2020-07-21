
package io.automatik.engine.workflow.process.executable.core.factory;

import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public abstract class NodeFactory {

	public static final String METHOD_NAME = "name";
	public static final String METHOD_METADATA = "metaData";
	public static final String METHOD_DONE = "done";

	private Node node;
	private NodeContainer nodeContainer;
	protected ExecutableNodeContainerFactory nodeContainerFactory;

	protected NodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		this.nodeContainerFactory = nodeContainerFactory;
		this.nodeContainer = nodeContainer;
		this.node = createNode();
		this.node.setId(id);
	}

	protected abstract Node createNode();

	protected Node getNode() {
		return node;
	}

	public NodeFactory name(String name) {
		getNode().setName(name);
		return this;
	}

	public NodeFactory metaData(String name, Object value) {
		getNode().setMetaData(name, value);
		return this;
	}

	public ExecutableNodeContainerFactory done() {
		nodeContainer.addNode(node);
		return this.nodeContainerFactory;
	}
}
