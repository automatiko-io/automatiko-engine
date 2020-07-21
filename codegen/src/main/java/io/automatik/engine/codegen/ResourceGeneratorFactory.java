
package io.automatik.engine.codegen;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.codegen.context.ApplicationBuildContext;
import io.automatik.engine.codegen.context.QuarkusApplicationBuildContext;
import io.automatik.engine.codegen.process.AbstractResourceGenerator;
import io.automatik.engine.codegen.process.ReactiveResourceGenerator;
import io.automatik.engine.codegen.process.ResourceGenerator;

public class ResourceGeneratorFactory {

	enum GeneratorType {
		QUARKUS(QuarkusApplicationBuildContext.class, false),
		QUARKUS_REACTIVE(QuarkusApplicationBuildContext.class, true);

		Class<? extends ApplicationBuildContext> buildContextClass;
		boolean reactive;

		GeneratorType(Class<? extends ApplicationBuildContext> buildContextClass, boolean reactive) {
			this.buildContextClass = buildContextClass;
			this.reactive = reactive;
		}

		public static Optional<GeneratorType> from(GeneratorContext context) {
			return Arrays.stream(GeneratorType.values())
					.filter(v -> Objects.equals(v.reactive, isReactiveGenerator(context)))
					.filter(v -> v.buildContextClass.isInstance(context.getBuildContext())).findFirst();
		}

		static boolean isReactiveGenerator(GeneratorContext context) {
			return "reactive"
					.equals(context.getApplicationProperty(GeneratorConfig.REST_RESOURCE_TYPE_PROP).orElse(""));
		}
	}

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