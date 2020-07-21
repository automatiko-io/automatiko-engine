
package io.automatik.engine.workflow.process.core.node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;

/**
 * Default implementation of an action node.
 * 
 */
public class ActionNode extends ExtendedNodeImpl {

	private static final long serialVersionUID = 510l;

	private ProcessAction action;
	private List<DataAssociation> inMapping = new LinkedList<DataAssociation>();
	private List<DataAssociation> outMapping = new LinkedList<DataAssociation>();

	public ProcessAction getAction() {
		return action;
	}

	public void setAction(ProcessAction action) {
		this.action = action;
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		super.validateAddIncomingConnection(type, connection);
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
					+ ", " + connection.getTo().getName() + "] only accepts default incoming connection type!");
		}
		if (getFrom() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
			throw new IllegalArgumentException("This type of node [" + connection.getTo().getMetaData().get("UniqueId")
					+ ", " + connection.getTo().getName() + "] cannot have more than one incoming connection!");
		}
	}

	public void validateAddOutgoingConnection(final String type, final Connection connection) {
		super.validateAddOutgoingConnection(type, connection);
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException(
					"This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
							+ connection.getFrom().getName() + "] only accepts default outgoing connection type!");
		}
		if (getTo() != null && !"true".equals(System.getProperty("jbpm.enable.multi.con"))) {
			throw new IllegalArgumentException(
					"This type of node [" + connection.getFrom().getMetaData().get("UniqueId") + ", "
							+ connection.getFrom().getName() + "] cannot have more than one outgoing connection!");
		}
	}

	public void addInAssociation(DataAssociation dataAssociation) {
		inMapping.add(dataAssociation);
	}

	public List<DataAssociation> getInAssociations() {
		return Collections.unmodifiableList(inMapping);
	}

	public void addOutAssociation(DataAssociation dataAssociation) {
		outMapping.add(dataAssociation);
	}

	public List<DataAssociation> getOutAssociations() {
		return Collections.unmodifiableList(outMapping);
	}

}
