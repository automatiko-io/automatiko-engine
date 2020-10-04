package io.automatik.engine.quarkus.deployment;

import io.automatik.engine.codegen.GeneratorContext;
import io.automatik.engine.quarkus.AutomatikBuildTimeConfig;

public class AutomatikBuildData {

    private static AutomatikBuildData INSTANCE;

    protected AutomatikBuildTimeConfig config;
    protected GeneratorContext generationContext;

    private AutomatikBuildData(AutomatikBuildTimeConfig config, GeneratorContext generationContext) {
        this.config = config;
        this.generationContext = generationContext;
    }

    public static AutomatikBuildData create(AutomatikBuildTimeConfig config, GeneratorContext generationContext) {
        if (INSTANCE == null) {
            INSTANCE = new AutomatikBuildData(config, generationContext);
        }

        return INSTANCE;
    }

    public static AutomatikBuildData get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AutomatikBuildData was not yet created");
        }
        return INSTANCE;
    }

    public AutomatikBuildTimeConfig getConfig() {
        return config;
    }

    public GeneratorContext getGenerationContext() {
        return generationContext;
    }
}
