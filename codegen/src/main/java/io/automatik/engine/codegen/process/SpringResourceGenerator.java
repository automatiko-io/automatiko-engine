
package io.automatik.engine.codegen.process;

import java.util.Arrays;
import java.util.List;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.GeneratorContext;

/**
 * REST resources generator based on Spring Web API
 * {@linkplain https://docs.spring.io/spring/docs/current/spring
 * -framework-reference/web.html#spring-web}. The implementation is based on
 * template files to generated the endpoints that are handled on
 * {@link AbstractResourceGenerator}.
 */
public class SpringResourceGenerator extends AbstractResourceGenerator {

	private static final String RESOURCE_TEMPLATE = "/class-templates/spring/SpringRestResourceTemplate.java";

	public SpringResourceGenerator(GeneratorContext context, WorkflowProcess process, String modelfqcn,
			String processfqcn, String appCanonicalName) {
		super(context, process, modelfqcn, processfqcn, appCanonicalName);
	}

	@Override
	protected String getResourceTemplate() {
		return RESOURCE_TEMPLATE;
	}

	@Override
	public String getUserTaskResourceTemplate() {
		return "/class-templates/spring/SpringRestResourceUserTaskTemplate.java";
	}

	@Override
	protected String getSignalResourceTemplate() {
		return "/class-templates/spring/SpringRestResourceSignalTemplate.java";
	}

	@Override
	public List<String> getRestAnnotations() {
		return Arrays.asList("PostMapping", "GetMapping", "PutMapping", "DeleteMapping");
	}
}