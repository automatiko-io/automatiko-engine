
package io.automatik.engine.workflow.serverless.api.mapper;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import io.automatik.engine.workflow.serverless.api.WorkflowPropertySource;

public class YamlObjectMapper extends BaseObjectMapper {
	public YamlObjectMapper() {
		this(null);
	}

	public YamlObjectMapper(WorkflowPropertySource context) {
		super((new YAMLFactory()).enable(Feature.MINIMIZE_QUOTES), context);
	}
}
