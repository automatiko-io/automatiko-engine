
package io.automatik.engine.workflow.process.executable.instance;

import java.util.List;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatik.engine.workflow.process.instance.node.StartNodeInstance;

public class ExecutableProcessInstance extends WorkflowProcessInstanceImpl {

	private static final long serialVersionUID = 510l;

	public ExecutableProcess getRuleFlowProcess() {
		return (ExecutableProcess) getProcess();
	}

	public void internalStart(String trigger, Object triggerData) {
		StartNode startNode = getRuleFlowProcess().getStart(trigger);
		if (startNode != null) {
			if (trigger != null) {
				((StartNodeInstance) getNodeInstance(startNode)).signalEvent(trigger, triggerData);
			} else {

				getNodeInstance(startNode).trigger(null, null);
			}
		} else if (!getRuleFlowProcess().isDynamic()) {
			throw new IllegalArgumentException(
					"There is no start node that matches the trigger " + (trigger == null ? "none" : trigger));
		}

		// activate ad hoc fragments if they are marked as such
		List<Node> autoStartNodes = getRuleFlowProcess().getAutoStartNodes();
		autoStartNodes.forEach(autoStartNode -> signalEvent(autoStartNode.getName(), null));
	}

}
