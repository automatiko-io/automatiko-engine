
package io.automatiko.engine.workflow.process.core.node;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

public class TimerNode extends ExtendedNodeImpl {

	private static final long serialVersionUID = 510l;

	private Timer timer;

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public Timer getTimer() {
		return this.timer;
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		super.validateAddIncomingConnection(type, connection);
		if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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
		if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
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

}
