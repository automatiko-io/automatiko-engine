
package io.automatik.engine.workflow.serverless.api.mapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;

public class BaseObjectMapper extends ObjectMapper {

	private WorkflowModule workflowModule;

	public BaseObjectMapper(JsonFactory factory, WorkflowPropertySource workflowPropertySource) {
		super(factory);

		workflowModule = new WorkflowModule((workflowPropertySource));

		configure(SerializationFeature.INDENT_OUTPUT, true);
		registerModule(workflowModule);
	}

	public WorkflowModule getWorkflowModule() {
		return workflowModule;
	}
}