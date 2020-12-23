
package io.automatiko.engine.workflow.process.core.node;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

/**
 * Default implementation of a fault node.
 * 
 */
public class FaultNode extends ExtendedNodeImpl {

	private static final String[] EVENT_TYPES = new String[] { EVENT_NODE_ENTER };

	private static final long serialVersionUID = 510l;

	private String faultName;
	private String faultVariable;
	private boolean terminateParent = false;

	public String getFaultVariable() {
		return faultVariable;
	}

	public void setFaultVariable(String faultVariable) {
		this.faultVariable = faultVariable;
	}

	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}

	public boolean isTerminateParent() {
		return terminateParent;
	}

	public void setTerminateParent(boolean terminateParent) {
		this.terminateParent = terminateParent;
	}

	public String[] getActionTypes() {
		return EVENT_TYPES;
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		super.validateAddIncomingConnection(type, connection);
		if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
					+ ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
		}
		if (getFrom() != null) {
			throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
					+ ", " + connection.getTo().getName() + "] cannot have more than one incoming connection!");
		}
	}

	public void validateAddOutgoingConnection(final String type, final Connection connection) {
		throw new UnsupportedOperationException("A fault node does not have an outgoing connection!");
	}

	public void validateRemoveOutgoingConnection(final String type, final Connection connection) {
		throw new UnsupportedOperationException("A fault node does not have an outgoing connection!");
	}
}
