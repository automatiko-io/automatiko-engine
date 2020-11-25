
package io.automatik.engine.workflow.process.core.node;

import static io.automatik.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import io.automatik.engine.api.definition.process.Connection;

/**
 * Default implementation of a milestone node.
 */
public class MilestoneNode extends StateBasedNode implements Constrainable {

    private static final long serialVersionUID = 510L;

    /**
     * String representation of the conditionPredicate. Not used at runtime
     */
    private String condition;

    public void setConditionExpression(String condition) {
        this.condition = condition;
    }

    public String getConditionExpression() {
        return condition;
    }

    @Override
    public boolean hasCondition() {
        return false;
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
