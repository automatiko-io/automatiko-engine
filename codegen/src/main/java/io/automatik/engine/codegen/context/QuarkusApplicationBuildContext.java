
package io.automatik.engine.codegen.context;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import io.automatik.engine.api.config.AutomatikConfig;

public class QuarkusApplicationBuildContext implements ApplicationBuildContext {

    private AutomatikConfig config;
    private Predicate<String> classAvailabilityResolver;
    private Function<String, List<String>> implementationFinder;

    public QuarkusApplicationBuildContext(AutomatikConfig config, Predicate<String> classAvailabilityResolver,
            Function<String, List<String>> implementationFinder) {
        this.config = config;
        this.classAvailabilityResolver = classAvailabilityResolver;
        this.implementationFinder = implementationFinder;
    }

    @Override
    public boolean hasClassAvailable(String fqcn) {
        return classAvailabilityResolver.test(fqcn);
    }

    @Override
    public AutomatikConfig config() {
        return config;
    }

    @Override
    public List<String> classThatImplement(String fqcn) {
        return implementationFinder.apply(fqcn);
    }
}