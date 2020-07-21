
package io.automatik.engine.workflow.serverless.api;

public interface WorkflowValidator {

	WorkflowValidator setWorkflowManager(WorkflowManager workflowManager);

	void setJson(String json);

	void setYaml(String yaml);

	boolean isValid();

	void setEnabled(boolean enabled);

	void setSchemaValidationEnabled(boolean schemaValidationEnabled);

	void setStrictValidationEnabled(boolean strictValidationEnabled);

	void reset();
}