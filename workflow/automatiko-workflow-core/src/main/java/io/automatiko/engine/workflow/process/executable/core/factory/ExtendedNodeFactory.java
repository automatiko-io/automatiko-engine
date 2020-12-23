
package io.automatiko.engine.workflow.process.executable.core.factory;

import java.util.ArrayList;
import java.util.List;

import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public abstract class ExtendedNodeFactory extends NodeFactory {

	protected ExtendedNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected ExtendedNodeImpl getExtendedNode() {
		return (ExtendedNodeImpl) getNode();
	}

	public ExtendedNodeFactory onEntryAction(String dialect, String action) {
		if (getExtendedNode().getActions(dialect) != null) {
			getExtendedNode().getActions(dialect).add(new ConsequenceAction(dialect, action));
		} else {
			List<ProcessAction> actions = new ArrayList<>();
			actions.add(new ConsequenceAction(dialect, action));
			getExtendedNode().setActions(ExtendedNodeImpl.EVENT_NODE_ENTER, actions);
		}
		return this;
	}

	public ExtendedNodeFactory onExitAction(String dialect, String action) {
		if (getExtendedNode().getActions(dialect) != null) {
			getExtendedNode().getActions(dialect).add(new ConsequenceAction(dialect, action));
		} else {
			List<ProcessAction> actions = new ArrayList<>();
			actions.add(new ConsequenceAction(dialect, action));
			getExtendedNode().setActions(ExtendedNodeImpl.EVENT_NODE_EXIT, actions);
		}
		return this;
	}
}
