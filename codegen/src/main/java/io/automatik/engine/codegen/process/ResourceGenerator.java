
package io.automatik.engine.codegen.process;

import java.util.Arrays;
import java.util.List;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;

public class ResourceGenerator extends AbstractResourceGenerator {

	private static final String RESOURCE_TEMPLATE = "/class-templates/RestResourceTemplate.java";

	public ResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn, String processfqcn,
			String appCanonicalName) {
		super(context, process, modelfqcn, processfqcn, appCanonicalName);
	}

	@Override
	protected String getResourceTemplate() {
		return RESOURCE_TEMPLATE;
	}

	@Override
	public String getUserTaskResourceTemplate() {
		return "/class-templates/RestResourceUserTaskTemplate.java";
	}

	@Override
	protected String getSignalResourceTemplate() {
		return "/class-templates/RestResourceSignalTemplate.java";
	}

	@Override
	public List<String> getRestAnnotations() {
		return Arrays.asList("POST", "GET", "PUT", "DELETE");
	}
}