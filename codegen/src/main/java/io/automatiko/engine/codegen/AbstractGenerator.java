
package io.automatiko.engine.codegen;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGenerator implements Generator {

    protected Path projectDirectory;
    protected GeneratorContext context;

    protected AbstractGenerator() {
    }

    @Override
    public void setProjectDirectory(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    @Override
    public void setContext(GeneratorContext context) {
        this.context = context;
    }

    @Override
    public GeneratorContext context() {
        return this.context;
    }

    @Override
    public final Map<String, String> getLabels() {
        final Map<String, String> labels = new HashMap<>();
        return labels;
    }

    public boolean isServiceProject() {
        return context.getBuildContext().hasClassAvailable("javax.ws.rs.Path") || onClasspath("javax.ws.rs.Path");
    }

    public boolean isFunctionProject() {
        return context.getBuildContext().hasClassAvailable("io.quarkus.funqy.Funq") || onClasspath("io.quarkus.funqy.Funq");
    }

    public boolean isFunctionFlowProject() {
        return context.getBuildContext().hasClassAvailable("io.quarkus.funqy.knative.events.CloudEventMapping")
                || onClasspath("io.quarkus.funqy.knative.events.CloudEventMapping");
    }

    protected boolean onClasspath(String clazz) {
        try {
            Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
