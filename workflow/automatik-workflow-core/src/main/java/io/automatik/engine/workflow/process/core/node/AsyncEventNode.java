
package io.automatik.engine.workflow.process.core.node;

import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;

public class AsyncEventNode extends EventNode {

	private static final long serialVersionUID = -4724021457443413412L;

	private Node node;

	public AsyncEventNode(Node node) {
		this.node = node;
	}

	public Node getActualNode() {
		return node;
	}

	@Override
	public long getId() {
		return node.getId();
	}

	@Override
	public Object getMetaData(String name) {
		return ((NodeImpl) node).getMetaData(name);
	}

	@Override
	public Map<String, Object> getMetaData() {
		Map<String, Object> metaData = new HashMap<String, Object>(node.getMetaData());
		metaData.put("hidden", "true");
		return metaData;
	}

}
