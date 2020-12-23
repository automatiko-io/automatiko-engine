
package io.automatiko.engine.workflow.process.core.impl;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.process.core.NodeContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class NodeContainerImpl implements NodeContainer {

	private static final long serialVersionUID = 510l;

	private Map<Long, Node> nodes;

	public NodeContainerImpl() {
		this.nodes = new HashMap<Long, Node>();
	}

	public void addNode(final Node node) {
		validateAddNode(node);
		if (!this.nodes.containsValue(node)) {
			this.nodes.put(new Long(node.getId()), node);
		}
	}

	protected void validateAddNode(Node node) {
		if (node == null) {
			throw new IllegalArgumentException("Node is null!");
		}
	}

	public Node[] getNodes() {
		return (Node[]) this.nodes.values().toArray(new Node[this.nodes.size()]);
	}

	public Node getNode(final long id) {
		Node node = this.nodes.get(id);
		if (node == null) {
			throw new IllegalArgumentException("Unknown node id: " + id);
		}
		return node;
	}

	public Node internalGetNode(long id) {
		return getNode(id);
	}

	public void removeNode(final Node node) {
		validateRemoveNode(node);
		this.nodes.remove(new Long(node.getId()));
	}

	protected void validateRemoveNode(Node node) {
		if (node == null) {
			throw new IllegalArgumentException("Node is null");
		}
		if (this.nodes.get(node.getId()) == null) {
			throw new IllegalArgumentException("Unknown node: " + node);
		}
	}

	public Context resolveContext(String contextId, Object param) {
		return null;
	}

}
