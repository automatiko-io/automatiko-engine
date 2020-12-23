
package io.automatiko.engine.codegen;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.codegen.process.AbstractResourceGenerator;
import io.automatiko.engine.codegen.process.ReactiveResourceGenerator;
import io.automatiko.engine.codegen.process.ResourceGenerator;

/**
 * This should be used to only create JAX-RS Resource Generators. IMPORTANT: it
 * will not consider Spring Generators.
 */
public class DefaultResourceGeneratorFactory extends ResourceGeneratorFactory {

	@Override
	public Optional<AbstractResourceGenerator> create(GeneratorContext context, WorkflowProcess process,
			String modelfqcn, String processfqcn, String appCanonicalName) {

		return GeneratorType.from(context).map(type -> {
			switch (type) {
			case QUARKUS:
				return new ResourceGenerator(context, process, modelfqcn, processfqcn, appCanonicalName);
			case QUARKUS_REACTIVE:
				return new ReactiveResourceGenerator(context, process, modelfqcn, processfqcn, appCanonicalName);
			default:
				throw new NoSuchElementException("No Resource Generator for: " + type);
			}
		});
	}
}