
package io.automatik.engine.workflow.process.core.node;

import static io.automatik.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.function.Predicate;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.runtime.process.ProcessContext;

/**
 * Default implementation of a milestone node.
 */
public class MilestoneNode extends StateBasedNode implements Constrainable {

	private static final long serialVersionUID = 510L;

	/**
	 * String representation of the conditionPredicate. Not used at runtime
	 */
	private String condition;
	private Predicate<ProcessContext> conditionPredicate;

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(Predicate<ProcessContext> conditionPredicate) {
		this.conditionPredicate = conditionPredicate;
	}

	public boolean canComplete(ProcessContext context) {
		return conditionPredicate == null || conditionPredicate.test(context);
	}

	public void validateAddIncomingConnection(final String type, final Connection connection) {
		super.validateAddIncomingConnection(type, connection);
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throwValidationException(connection, "only accepts default incoming connection type!");
		}
		if (getFrom() != null && !Boolean.parseBoolean(System.getProperty("jbpm.enable.multi.con"))) {
			throwValidationException(connection, "cannot have more than one incoming connection!");
		}
	}

	public void validateAddOutgoingConnection(final String type, final Connection connection) {
		super.validateAddOutgoingConnection(type, connection);
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throwValidationException(connection, "only accepts default outgoing connection type!");
		}
		if (getTo() != null && !Boolean.parseBoolean(System.getProperty("jbpm.enable.multi.con"))) {
			throwValidationException(connection, "cannot have more than one outgoing connection!");
		}
	}

	private static void throwValidationException(Connection connection, String msg) {
		throw new IllegalArgumentException("This type of node [" + connection.getFrom().getMetaData().get(UNIQUE_ID)
				+ ", " + connection.getFrom().getName() + "] " + msg);
	}

}
