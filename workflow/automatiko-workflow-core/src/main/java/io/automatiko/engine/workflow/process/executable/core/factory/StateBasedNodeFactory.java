
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.StateBasedNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public abstract class StateBasedNodeFactory extends ExtendedNodeFactory {

	public static final String METHOD_TIMER = "timer";

	protected StateBasedNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	protected StateBasedNode getStateBasedNode() {
		return (StateBasedNode) getNode();
	}

	public StateBasedNodeFactory timer(String delay, String period, String dialect, String action) {
		Timer timer = new Timer();
		timer.setDelay(delay);
		timer.setPeriod(period);
		getStateBasedNode().addTimer(timer, new ConsequenceAction(dialect, action));
		return this;
	}
}