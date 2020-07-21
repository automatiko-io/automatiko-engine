
package io.automatik.engine.workflow.serverless.api;

import io.automatik.engine.workflow.serverless.api.events.EventDefinition;

public interface ExpressionEvaluator {

	String getName();

	boolean evaluate(String expression, EventDefinition eventDefinition);
}