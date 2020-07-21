
package io.automatik.engine.workflow.process.instance.node;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.process.core.Constraint;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;

public class StateNodeInstance extends CompositeContextNodeInstance implements EventListener {

	private static final long serialVersionUID = 510l;

	protected StateNode getStateNode() {
		return (StateNode) getNode();
	}

	@Override
	public void internalTrigger(NodeInstance from, String type) {
		super.internalTrigger(from, type);
		// if node instance was cancelled, abort
		if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
			return;
		}
		// TODO: composite states trigger
		StateNode stateNode = getStateNode();
		Connection selected = null;
		int priority = Integer.MAX_VALUE;
		for (Connection connection : stateNode.getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE)) {
			Constraint constraint = stateNode.getConstraint(connection);
			if (constraint != null && constraint.getPriority() < priority) {

			}
		}
		if (selected != null) {
			((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
			triggerConnection(selected);
		} else {
			addTriggerListener();
			addActivationListener();
		}
	}

	protected boolean isLinkedIncomingNodeRequired() {
		return false;
	}

	public void signalEvent(String type, Object event) {
		if ("signal".equals(type)) {
			if (event instanceof String) {
				for (Connection connection : getStateNode().getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE)) {
					boolean selected = false;
					Constraint constraint = getStateNode().getConstraint(connection);
					if (constraint == null) {
						if (((String) event).equals(connection.getTo().getName())) {
							selected = true;
						}
					} else if (((String) event).equals(constraint.getName())) {
						selected = true;
					}
					if (selected) {
						triggerEvent(ExtendedNodeImpl.EVENT_NODE_EXIT);
						removeEventListeners();
						((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
								.removeNodeInstance(this);
						triggerConnection(connection);
						return;
					}
				}
			}
		} else {
			super.signalEvent(type, event);
		}
	}

	private void addTriggerListener() {
		getProcessInstance().addEventListener("signal", this, false);
	}

	private void addActivationListener() {
		getProcessInstance().addEventListener(getActivationEventType(), this, true);
	}

	public void addEventListeners() {
		super.addEventListeners();
		addTriggerListener();
		addActivationListener();
	}

	public void removeEventListeners() {
		super.removeEventListeners();
		getProcessInstance().removeEventListener("signal", this, false);
		getProcessInstance().removeEventListener(getActivationEventType(), this, true);
	}

	public String[] getEventTypes() {
		return new String[] { "signal", getActivationEventType() };
	}

	private String getActivationEventType() {
		return "RuleFlowStateNode-" + getProcessInstance().getProcessId() + "-" + getStateNode().getUniqueId();
	}

}
