
package io.automatik.engine.workflow.process.test;

import java.lang.reflect.Constructor;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.impl.WorkImpl;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class NodeCreator<T extends NodeImpl> {
	NodeContainer nodeContainer;
	Constructor<T> constructor;

	private static long idGen = 1;

	public NodeCreator(NodeContainer nodeContainer, Class<T> clazz) {
		this.nodeContainer = nodeContainer;
		this.constructor = (Constructor<T>) clazz.getConstructors()[0];
	}

	public T createNode(String name) throws Exception {
		T result = this.constructor.newInstance(new Object[0]);
		result.setId(idGen++);
		result.setName(name);
		this.nodeContainer.addNode(result);

		if (result instanceof WorkItemNode) {
			Work work = new WorkImpl();
			((WorkItemNode) result).setWork(work);
		}
		return result;
	}

	public void setNodeContainer(NodeContainer newNodeContainer) {
		this.nodeContainer = newNodeContainer;
	}

	public static void connect(Node nodeOne, Node nodeTwo) {
		new ConnectionImpl(nodeOne, Node.CONNECTION_DEFAULT_TYPE, nodeTwo, Node.CONNECTION_DEFAULT_TYPE);
	}
}