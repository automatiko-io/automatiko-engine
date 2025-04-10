package io.automatiko.engine.quarkus.functionflow.deployment.generators;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;
import io.automatiko.engine.quarkus.functionflow.deployment.AutomatikoFunctionFlowProcessor;
import io.automatiko.engine.quarkus.functionflow.deployment.ExampleGenerator;
import io.automatiko.engine.services.utils.StringUtils;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class DefaultGenerator implements Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGenerator.class);

    public void generate(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob, AutomatikoBuildTimeConfig config) throws IOException {
        Path outputdir = bts.getOutputDirectory();
        Path directory = Paths.get(outputdir.toString(), "functions");
        Files.createDirectories(directory);

        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = AutomatikoFunctionFlowProcessor.buildAnnotationScannerContext(index.getIndex());

        StringBuilder descriptorFileContent = new StringBuilder();

        // create function sink binding descriptor
        String sinkBindingTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-sink-template.yaml")));
        descriptorFileContent.append(sinkBindingTemplate
                .replaceAll("@@name@@", cob.getApplicationModel().getAppArtifact().getArtifactId()));

        descriptorFileContent.append("\n").append("---").append("\n");

        // create function service descriptor
        String serviceTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-service-template.yaml")));
        descriptorFileContent.append(serviceTemplate
                .replaceAll("@@name@@", cob.getApplicationModel().getAppArtifact().getArtifactId())
                .replaceAll("@@user@@", System.getProperty("user.name"))
                .replaceAll("@@version@@", cob.getApplicationModel().getAppArtifact().getVersion()));

        Collection<AnnotationInstance> functions = index.getIndex()
                .getAnnotations(AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");
        DotName generatedData = AutomatikoFunctionFlowProcessor.createDotName("io.automatiko.engine.api.codegen.Generated");

        LOGGER.info("************************************************************");
        LOGGER.info("***********Automatiko Function Flow Instructions************");
        LOGGER.info("************************************************************");

        // for each found function generate sample payload and print out as instructions
        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();

                if (!mi.declaringClass().hasDeclaredAnnotation(generatedData)) {
                    continue;
                }

                // create function trigger descriptor for every found function
                String triggerTemplate = StringUtils.readFileAsString(
                        new InputStreamReader(this.getClass().getResourceAsStream("/function-trigger-template.yaml")));
                descriptorFileContent.append("\n").append("---").append("\n");
                descriptorFileContent.append(triggerTemplate
                        .replaceAll("@@name@@", sanitizeIdentifier(mi.name()))
                        .replaceAll("@@trigger@@", mi.annotation(mapping).value("trigger").asString())
                        .replaceAll("@@servicename@@", cob.getApplicationModel().getAppArtifact().getArtifactId()));
                boolean includeSubjectAttribute = false;
                Type param = mi.parameters().get(0).type();
                if (param instanceof ParameterizedType) {
                    param = ((ParameterizedType) param).arguments().get(0);
                    includeSubjectAttribute = true;
                }

                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0).type(), null);
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas().get(param.name().local());
                LOGGER.info(
                        "Function \"{}\" will accept POST requests on / endpoint with following payload ",
                        mi.name());
                Stream.of(generator.generate(fSchema, ctx.getOpenApi()).split("\\r?\\n")).forEach(LOGGER::info);
                LOGGER.info("(as either binary or structured cloud event of type \"{}\") ",
                        mi.annotation(mapping).value("trigger").asString());
                if (includeSubjectAttribute) {
                    LOGGER.info("IMPORTANT: include 'subject' attribute of cloud event that points to workflow instance id");
                }
                LOGGER.info("*****************************************");
            }
        }

        Path filePath = Paths.get(directory.toString(),
                cob.getApplicationModel().getAppArtifact().getArtifactId() + "-"
                        + cob.getApplicationModel().getAppArtifact().getVersion() + ".yaml");
        Files.write(
                filePath,
                descriptorFileContent.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Complete deployment file is located at {}", filePath.toAbsolutePath());
        LOGGER.info("************************************************************");

    }

    private String sanitizeIdentifier(String name) {
        return name.replaceAll("\\p{P}", "").toLowerCase();
    }
}
