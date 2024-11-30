package io.automatiko.engine.quarkus.functionflow.deployment.generators;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;
import io.automatiko.engine.quarkus.functionflow.deployment.AutomatikoFunctionFlowProcessor;
import io.automatiko.engine.quarkus.functionflow.deployment.ExampleGenerator;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class GcpPubSubGenerator implements Generator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPubSubGenerator.class);

    public void generate(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob, AutomatikoBuildTimeConfig config) throws IOException {
        Path outputdir = bts.getOutputDirectory();
        Path directory = Paths.get(outputdir.toString(), "scripts");
        Files.createDirectories(directory);

        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = AutomatikoFunctionFlowProcessor.buildAnnotationScannerContext(index.getIndex());

        List<String> createCommands = new ArrayList<>();
        List<String> deleteCommands = new ArrayList<>();

        Collection<AnnotationInstance> functions = index.getIndex()
                .getAnnotations(AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = AutomatikoFunctionFlowProcessor.createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");

        DotName generatedData = AutomatikoFunctionFlowProcessor.createDotName("io.automatiko.engine.api.codegen.Generated");

        LOGGER.info("************************************************************");
        LOGGER.info("***********Automatiko Function Flow Instructions************");
        LOGGER.info("************************************************************");
        Set<ClassInfo> functionClasses = new HashSet<>();

        // for each found function generate sample payload and print out as instructions
        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();

                if (!mi.declaringClass().hasDeclaredAnnotation(generatedData)) {
                    continue;
                }

                functionClasses.add(mi.declaringClass());
                String topic = ((AnnotationInstance) ((AnnotationValue[]) mi.annotation(mapping).value("attributes").value())[0]
                        .value()).value().asString();
                createCommands.add("gcloud eventarc triggers create " + mi.name()
                        + " --event-filters=\"type=google.cloud.pubsub.topic.v1.messagePublished\" --destination-run-service="
                        + cob.getApplicationModel().getAppArtifact().getArtifactId()
                        + " --destination-run-path=/ --transport-topic=" + topic.substring(topic.lastIndexOf("/") + 1)
                        + " --location=us-central1");

                deleteCommands.add("gcloud eventarc triggers delete " + mi.name() + " --location=us-central1");

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
        createCommands.add("");
        deleteCommands.add("");

        String googleProjectId = "";
        for (ClassInfo clz : functionClasses) {
            AnnotationInstance genAnnotation = clz.declaredAnnotation(generatedData);

            if (genAnnotation != null) {
                googleProjectId = genAnnotation.value("reference").asString();
                String[] functionIds = genAnnotation.value().asStringArray();
                for (String functionId : functionIds) {
                    createCommands.add("gcloud pubsub topics create " + functionId + " --project=" + googleProjectId);
                    deleteCommands.add("gcloud pubsub topics delete " + functionId + " --project=" + googleProjectId);
                }

            }

        }

        Collections.reverse(createCommands);
        Collections.reverse(deleteCommands);

        createCommands.add("");
        deleteCommands.add("");

        createCommands.add("gcloud run deploy " + cob.getApplicationModel().getAppArtifact().getArtifactId()
                + " --platform=managed --image=gcr.io/" + googleProjectId + "/"
                + System.getProperty("user.name") + "/" + cob.getApplicationModel().getAppArtifact().getArtifactId() + ":"
                + cob.getApplicationModel().getAppArtifact().getVersion()
                + " --region=us-central1");

        deleteCommands.add("gcloud run services delete " + cob.getApplicationModel().getAppArtifact().getArtifactId()
                + " --region=us-central1");

        Path filePath = Paths.get(directory.toString(),
                "deploy-" + cob.getApplicationModel().getAppArtifact().getArtifactId() + "-"
                        + cob.getApplicationModel().getAppArtifact().getVersion() + ".txt");
        Files.write(
                filePath,
                createCommands.stream().collect(Collectors.joining(System.lineSeparator())).getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Complete deployment file is located at {}", filePath.toAbsolutePath());
        filePath = Paths.get(directory.toString(),
                "undeploy-" + cob.getApplicationModel().getAppArtifact().getArtifactId() + "-"
                        + cob.getApplicationModel().getAppArtifact().getVersion() + ".txt");
        Files.write(
                filePath,
                deleteCommands.stream().collect(Collectors.joining(System.lineSeparator())).getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Complete undeployment file is located at {}", filePath.toAbsolutePath());
        LOGGER.info("************************************************************");

    }
}
