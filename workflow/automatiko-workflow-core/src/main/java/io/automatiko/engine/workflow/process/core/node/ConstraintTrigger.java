
package io.automatiko.engine.workflow.process.core.node;

import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;

public class ConstraintTrigger extends Trigger implements Constrainable {

	private static final long serialVersionUID = 510l;

	private String constraint;
	private String header;

	public String getConstraint() {
		return constraint;
	}

	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void addConstraint(ConnectionRef connection, Constraint constraint) {
		if (connection != null) {
			throw new IllegalArgumentException("A constraint trigger only accepts one simple constraint");
		}
		this.constraint = constraint.getConstraint();
	}

}
