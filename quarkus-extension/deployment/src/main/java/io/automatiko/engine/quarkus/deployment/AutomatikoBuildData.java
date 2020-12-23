package io.automatiko.engine.quarkus.deployment;

import io.automatiko.engine.codegen.GeneratorContext;
import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;

public class AutomatikoBuildData {

    private static AutomatikoBuildData INSTANCE;

    protected AutomatikoBuildTimeConfig config;
    protected GeneratorContext generationContext;

    private AutomatikoBuildData(AutomatikoBuildTimeConfig config, GeneratorContext generationContext) {
        this.config = config;
        this.generationContext = generationContext;
    }

    public static AutomatikoBuildData create(AutomatikoBuildTimeConfig config, GeneratorContext generationContext) {
        if (INSTANCE == null) {
            INSTANCE = new AutomatikoBuildData(config, generationContext);
        }

        return INSTANCE;
    }

    public static AutomatikoBuildData get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AutomatikBuildData was not yet created");
        }
        return INSTANCE;
    }

    public AutomatikoBuildTimeConfig getConfig() {
        return config;
    }

    public GeneratorContext getGenerationContext() {
        return generationContext;
    }
}
