package io.automatik.engine.workflow.serverless;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.process.core.NodeContainer;

public class TestNodeContainer implements NodeContainer {
	@Override
	public void addNode(Node node) {

	}

	@Override
	public void removeNode(Node node) {
	}

	@Override
	public Context resolveContext(String contextId, Object param) {
		return null;
	}

	@Override
	public Node internalGetNode(long id) {
		return null;
	}

	@Override
	public Node[] getNodes() {
		return new Node[0];
	}

	@Override
	public Node getNode(long id) {
		return null;
	}
}
