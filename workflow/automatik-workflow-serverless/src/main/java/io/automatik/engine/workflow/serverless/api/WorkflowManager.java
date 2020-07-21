
package io.automatik.engine.workflow.serverless.api;

import io.automatik.engine.workflow.serverless.api.interfaces.Extension;

public interface WorkflowManager {

	void setWorkflow(Workflow workflow);

	Workflow getWorkflow();

	WorkflowManager setMarkup(String markup);

	void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator);

	void setDefaultExpressionEvaluator(String evaluatorName);

	ExpressionEvaluator getExpressionEvaluator();

	ExpressionEvaluator getExpressionEvaluator(String evaluatorName);

	void resetExpressionValidator();

	WorkflowValidator getWorkflowValidator();

	String toJson();

	String toYaml();

	Workflow toWorkflow(String json);

	void registerExtension(String extensionId, Class<? extends Extension> extensionHandlerClass);
}