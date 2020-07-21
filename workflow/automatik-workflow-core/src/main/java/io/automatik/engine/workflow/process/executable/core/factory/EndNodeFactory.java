
package io.automatik.engine.workflow.process.executable.core.factory;

import static io.automatik.engine.workflow.process.executable.core.Metadata.ACTION;

import java.util.ArrayList;
import java.util.List;

import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class EndNodeFactory extends ExtendedNodeFactory {

	public static final String METHOD_TERMINATE = "terminate";
	public static final String METHOD_ACTION = "action";

	public EndNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new EndNode();
	}

	protected EndNode getEndNode() {
		return (EndNode) getNode();
	}

	@Override
	public EndNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	public EndNodeFactory terminate(boolean terminate) {
		getEndNode().setTerminate(terminate);
		return this;
	}

	public EndNodeFactory action(Action action) {
		ProcessAction processAction = new ProcessAction();
		processAction.setMetaData(ACTION, action);
		List<ProcessAction> enterActions = getEndNode().getActions(ExtendedNodeImpl.EVENT_NODE_ENTER);
		if (enterActions == null) {
			enterActions = new ArrayList<>();
			getEndNode().setActions(ExtendedNodeImpl.EVENT_NODE_ENTER, enterActions);
		}
		enterActions.add(processAction);
		return this;
	}
}
