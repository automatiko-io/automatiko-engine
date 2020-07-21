
package io.automatik.engine.workflow.base.instance.impl;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.workflow.process.core.Constraint;
import io.automatik.engine.workflow.process.instance.NodeInstance;

public interface ConstraintEvaluator extends Constraint {

	public boolean evaluate(NodeInstance instance, Connection connection, Constraint constraint);
}
