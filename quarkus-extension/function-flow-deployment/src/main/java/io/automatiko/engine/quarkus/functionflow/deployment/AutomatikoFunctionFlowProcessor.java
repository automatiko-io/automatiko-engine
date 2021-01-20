package io.automatiko.engine.quarkus.functionflow.deployment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.automatiko.engine.services.utils.StringUtils;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class AutomatikoFunctionFlowProcessor {

    private static final String FEATURE = "automatiko-function-flow";

    @BuildStep
    FeatureBuildItem generateFunctionFlowDeploymentFiles(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob) throws IOException {
        Path outputdir = bts.getOutputDirectory();

        // create function service descriptor
        String serviceTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-service-template.yaml")));
        String serviceDescriptor = serviceTemplate
                .replaceAll("@@name@@", cob.getEffectiveModel().getAppArtifact().getArtifactId())
                .replaceAll("@@user@@", System.getProperty("user.name"))
                .replaceAll("@@version@@", cob.getEffectiveModel().getAppArtifact().getVersion());
        Path directory = Paths.get(outputdir.toString(), "functions");
        Files.createDirectories(directory);
        Files.write(Paths.get(directory.toString(), "service-deployment.yaml"),
                serviceDescriptor.getBytes(StandardCharsets.UTF_8));

        // create function sink binding descriptor
        String sinkBindingTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-sink-template.yaml")));
        String ssinkBindingDescriptor = sinkBindingTemplate
                .replaceAll("@@name@@", cob.getEffectiveModel().getAppArtifact().getArtifactId());

        Files.write(Paths.get(directory.toString(), "service-sink-biding.yaml"),
                ssinkBindingDescriptor.getBytes(StandardCharsets.UTF_8));

        Collection<AnnotationInstance> functions = index.getIndex().getAnnotations(createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");
        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();
                // create function trigger descriptor for every found function
                String triggerTemplate = StringUtils.readFileAsString(
                        new InputStreamReader(this.getClass().getResourceAsStream("/function-trigger-template.yaml")));

                String triggerDescriptor = triggerTemplate
                        .replaceAll("@@name@@", mi.name())
                        .replaceAll("@@trigger@@", mi.annotation(mapping).value("trigger").asString())
                        .replaceAll("@@servicename@@", cob.getEffectiveModel().getAppArtifact().getArtifactId());

                Files.write(Paths.get(directory.toString(), "trigger-" + mi.name() + "-deployment.yaml"),
                        triggerDescriptor.getBytes(StandardCharsets.UTF_8));
            }
        }

        return new FeatureBuildItem(FEATURE);
    }

    private DotName createDotName(String name) {
        int lastDot = name.indexOf('.');
        if (lastDot < 0) {
            return DotName.createComponentized(null, name);
        }

        DotName lastDotName = null;
        while (lastDot > 0) {
            String local = name.substring(0, lastDot);
            name = name.substring(lastDot + 1);
            lastDot = name.indexOf('.');
            lastDotName = DotName.createComponentized(lastDotName, local);
        }

        int lastDollar = name.indexOf('$');
        if (lastDollar < 0) {
            return DotName.createComponentized(lastDotName, name);
        }
        DotName lastDollarName = null;
        while (lastDollar > 0) {
            String local = name.substring(0, lastDollar);
            name = name.substring(lastDollar + 1);
            lastDollar = name.indexOf('$');
            if (lastDollarName == null) {
                lastDollarName = DotName.createComponentized(lastDotName, local);
            } else {
                lastDollarName = DotName.createComponentized(lastDollarName, local, true);
            }
        }
        return DotName.createComponentized(lastDollarName, name, true);
    }
}
