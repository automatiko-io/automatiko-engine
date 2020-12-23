
package io.automatiko.engine.workflow.process.core.node;

import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;

public interface Constrainable {

	/**
	 * Adds the given constraint. In cases where the constraint is associated with a
	 * specific connection, this connection will be identified using a
	 * ConnectionRef. In other cases the ConnectionRef will be null and can be
	 * ignored.
	 * 
	 * @param connection
	 * @param constraint
	 */
	public void addConstraint(ConnectionRef connection, Constraint constraint);

}
