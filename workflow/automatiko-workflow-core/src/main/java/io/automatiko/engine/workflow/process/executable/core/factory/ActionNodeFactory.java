
package io.automatiko.engine.workflow.process.executable.core.factory;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.ACTION;

import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class ActionNodeFactory extends ExtendedNodeFactory {

	public static final String METHOD_ACTION = "action";

	public ActionNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected Node createNode() {
		return new ActionNode();
	}

	protected ActionNode getActionNode() {
		return (ActionNode) getNode();
	}

	@Override
	public ActionNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	public ActionNodeFactory action(String dialect, String action) {
		return action(dialect, action, false);
	}

	public ActionNodeFactory action(String dialect, String action, boolean isprocessAction) {
		if (isprocessAction) {
			ProcessAction processAction = new ProcessAction();
			processAction.setMetaData(ACTION, action);
			getActionNode().setAction(processAction);
		} else {
			getActionNode().setAction(new ConsequenceAction(dialect, action));
		}
		return this;
	}

	public ActionNodeFactory action(Action action) {
		ProcessAction processAction = new ProcessAction();
		processAction.setMetaData(ACTION, action);
		getActionNode().setAction(processAction);
		return this;
	}
}
