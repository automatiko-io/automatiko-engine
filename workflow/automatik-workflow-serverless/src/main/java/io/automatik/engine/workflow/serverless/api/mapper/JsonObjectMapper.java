
package io.automatik.engine.workflow.serverless.api.mapper;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;

public class JsonObjectMapper extends BaseObjectMapper {

	public JsonObjectMapper() {
		this(null);
	}

	public JsonObjectMapper(WorkflowPropertySource context) {
		super(null, context);
	}
}