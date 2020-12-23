
package io.automatiko.engine.codegen.process;

import java.util.Arrays;
import java.util.List;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.GeneratorContext;

public class SubprocessResourceGenerator extends AbstractResourceGenerator {

	private static final String RESOURCE_TEMPLATE = "/class-templates/SubprocessRestResourceTemplate.java";

	public SubprocessResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
			String processfqcn, String appCanonicalName) {
		super(context, process, modelfqcn, processfqcn, appCanonicalName);
	}

	@Override
	protected String getResourceTemplate() {
		return RESOURCE_TEMPLATE;
	}

	@Override
	public String getUserTaskResourceTemplate() {
		return "/class-templates/SubprocessRestResourceUserTaskTemplate.java";
	}

	@Override
	protected String getSignalResourceTemplate() {
		return "/class-templates/SubprocessRestResourceSignalTemplate.java";
	}

	@Override
	public List<String> getRestAnnotations() {
		return Arrays.asList("POST", "GET", "PUT", "DELETE");
	}
}