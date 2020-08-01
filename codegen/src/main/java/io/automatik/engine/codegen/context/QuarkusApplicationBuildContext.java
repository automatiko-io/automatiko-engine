
package io.automatik.engine.codegen.context;

import java.util.function.Predicate;

import io.automatik.engine.api.config.AutomatikConfig;

public class QuarkusApplicationBuildContext implements ApplicationBuildContext {

	private AutomatikConfig config;
	private Predicate<String> classAvailabilityResolver;

	public QuarkusApplicationBuildContext(AutomatikConfig config, Predicate<String> classAvailabilityResolver) {
		this.config = config;
		this.classAvailabilityResolver = classAvailabilityResolver;
	}

	@Override
	public boolean hasClassAvailable(String fqcn) {
		return classAvailabilityResolver.test(fqcn);
	}

	@Override
	public AutomatikConfig config() {
		return config;
	}
}