
package io.automatik.engine.codegen.context;

import java.util.function.Predicate;

public class QuarkusApplicationBuildContext implements ApplicationBuildContext {

	private Predicate<String> classAvailabilityResolver;

	public QuarkusApplicationBuildContext(Predicate<String> classAvailabilityResolver) {
		this.classAvailabilityResolver = classAvailabilityResolver;
	}

	@Override
	public boolean hasClassAvailable(String fqcn) {
		return classAvailabilityResolver.test(fqcn);
	}
}