
package io.automatiko.engine.workflow.process.core.node;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;

public class StateNode extends CompositeContextNode implements Constrainable {

    private static final long serialVersionUID = 510l;

    private Map<ConnectionRef, Constraint> constraints = new HashMap<ConnectionRef, Constraint>();

    private Predicate<ProcessContext> conditionPredicate;

    public void setConstraints(Map<ConnectionRef, Constraint> constraints) {
        this.constraints = constraints;
    }

    public void setConstraint(final Connection connection, final Constraint constraint) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (!getDefaultOutgoingConnections().contains(connection)) {
            throw new IllegalArgumentException("connection is unknown:" + connection);
        }
        addConstraint(new ConnectionRef((String) connection.getMetaData().get("UniqueId"), connection.getTo().getId(),
                connection.getToType()), constraint);
    }

    public void addConstraint(ConnectionRef connectionRef, Constraint constraint) {
        if (connectionRef == null) {
            throw new IllegalArgumentException("A state node only accepts constraints linked to a connection");
        }
        constraints.put(connectionRef, constraint);
    }

    public Constraint getConstraint(ConnectionRef connectionRef) {
        return constraints.get(connectionRef);
    }

    public Map<ConnectionRef, Constraint> getConstraints() {
        return constraints;
    }

    public Constraint getConstraint(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        ConnectionRef ref = new ConnectionRef((String) connection.getMetaData().get("UniqueId"),
                connection.getTo().getId(), connection.getToType());
        return this.constraints.get(ref);
    }

    @Override
    public boolean hasCondition() {
        return false;
    }

}
