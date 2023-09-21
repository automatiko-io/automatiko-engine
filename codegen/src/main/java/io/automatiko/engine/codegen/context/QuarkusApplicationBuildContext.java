
package io.automatiko.engine.codegen.context;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import io.automatiko.engine.api.config.AutomatikoBuildConfig;

public class QuarkusApplicationBuildContext implements ApplicationBuildContext {

    private AutomatikoBuildConfig config;
    private Predicate<String> classAvailabilityResolver;
    private Function<String, List<String>> implementationFinder;
    private Predicate<String> capabilityResolver;

    public QuarkusApplicationBuildContext(AutomatikoBuildConfig config, Predicate<String> classAvailabilityResolver,
            Function<String, List<String>> implementationFinder,
            Predicate<String> capabilityResolver) {
        this.config = config;
        this.classAvailabilityResolver = classAvailabilityResolver;
        this.implementationFinder = implementationFinder;
        this.capabilityResolver = capabilityResolver;
    }

    @Override
    public boolean hasClassAvailable(String fqcn) {
        return classAvailabilityResolver.test(fqcn);
    }

    @Override
    public AutomatikoBuildConfig config() {
        return config;
    }

    @Override
    public List<String> classThatImplement(String fqcn) {
        return implementationFinder.apply(fqcn);
    }

    @Override
    public boolean hasCapability(String capability) {
        return capabilityResolver.test(capability);
    }
}