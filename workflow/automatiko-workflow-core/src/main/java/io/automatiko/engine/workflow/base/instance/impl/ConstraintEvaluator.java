
package io.automatiko.engine.workflow.base.instance.impl;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.instance.NodeInstance;

public interface ConstraintEvaluator extends Constraint {

	public boolean evaluate(NodeInstance instance, Connection connection, Constraint constraint);
}
