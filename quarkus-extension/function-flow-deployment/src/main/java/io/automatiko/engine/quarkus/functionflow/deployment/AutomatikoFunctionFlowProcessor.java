package io.automatiko.engine.quarkus.functionflow.deployment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.services.utils.StringUtils;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.SchemaRegistry;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class AutomatikoFunctionFlowProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatikoFunctionFlowProcessor.class);

    private static final String FEATURE = "automatiko-function-flow";

    @BuildStep
    FeatureBuildItem generateFunctionFlowDeploymentFiles(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob) throws IOException {
        Path outputdir = bts.getOutputDirectory();
        Path directory = Paths.get(outputdir.toString(), "functions");
        Files.createDirectories(directory);

        ExampleGenerator generator = new ExampleGenerator();
        AnnotationScannerContext ctx = buildAnnotationScannerContext(index.getIndex());
        SchemaRegistry.newInstance(ctx);

        StringBuilder descriptorFileContent = new StringBuilder();

        // create function sink binding descriptor
        String sinkBindingTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-sink-template.yaml")));
        descriptorFileContent.append(sinkBindingTemplate
                .replaceAll("@@name@@", cob.getEffectiveModel().getAppArtifact().getArtifactId()));

        descriptorFileContent.append("\n").append("---").append("\n");

        // create function service descriptor
        String serviceTemplate = StringUtils.readFileAsString(
                new InputStreamReader(this.getClass().getResourceAsStream("/function-service-template.yaml")));
        descriptorFileContent.append(serviceTemplate
                .replaceAll("@@name@@", cob.getEffectiveModel().getAppArtifact().getArtifactId())
                .replaceAll("@@user@@", System.getProperty("user.name"))
                .replaceAll("@@version@@", cob.getEffectiveModel().getAppArtifact().getVersion()));

        Collection<AnnotationInstance> functions = index.getIndex().getAnnotations(createDotName("io.quarkus.funqy.Funq"));
        DotName mapping = createDotName("io.quarkus.funqy.knative.events.CloudEventMapping");

        LOGGER.info("************************************************************");
        LOGGER.info("***********Automatiko Function Flow Instructions************");
        LOGGER.info("************************************************************");

        // for each found function generate sample payload and print out as instructions
        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();
                // create function trigger descriptor for every found function
                String triggerTemplate = StringUtils.readFileAsString(
                        new InputStreamReader(this.getClass().getResourceAsStream("/function-trigger-template.yaml")));
                descriptorFileContent.append("\n").append("---").append("\n");
                descriptorFileContent.append(triggerTemplate
                        .replaceAll("@@name@@", mi.name())
                        .replaceAll("@@trigger@@", mi.annotation(mapping).value("trigger").asString())
                        .replaceAll("@@servicename@@", cob.getEffectiveModel().getAppArtifact().getArtifactId()));

                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0), Collections.emptyList());
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas().get(mi.parameters().get(0).name().local());
                LOGGER.info(
                        "Function \"{}\" will accept POST requests on / endpoint with following payload ",
                        mi.name());
                Stream.of(generator.generate(fSchema, ctx.getOpenApi()).split("\\r?\\n")).forEach(LOGGER::info);
                LOGGER.info("(as either binary or structured cloud event of type \"{}\") ",
                        mi.annotation(mapping).value("trigger").asString());
                LOGGER.info("*****************************************");
            }
        }

        Path filePath = Paths.get(directory.toString(),
                cob.getEffectiveModel().getAppArtifact().getArtifactId() + "-"
                        + cob.getEffectiveModel().getAppArtifact().getVersion() + ".yaml");
        Files.write(
                filePath,
                descriptorFileContent.toString().getBytes(StandardCharsets.UTF_8));

        LOGGER.info("Complete deployment file is located at {}", filePath.toAbsolutePath());
        LOGGER.info("************************************************************");

        SchemaRegistry.remove();
        return new FeatureBuildItem(FEATURE);
    }

    public static DotName createDotName(String name) {
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

    public static AnnotationScannerContext buildAnnotationScannerContext(IndexView index) {
        OpenApiConfig config = new OpenApiConfigImpl(new Config() {

            @Override
            public <T> T getValue(String propertyName, Class<T> propertyType) {
                return null;
            }

            @Override
            public Iterable<String> getPropertyNames() {
                return Collections.emptyList();
            }

            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return Optional.empty();
            }

            @Override
            public Iterable<ConfigSource> getConfigSources() {
                return Collections.emptyList();
            }

            @Override
            public ConfigValue getConfigValue(String propertyName) {
                return null;
            }

            @Override
            public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
                return Optional.empty();
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
        });
        AnnotationScannerContext ctx = new AnnotationScannerContext(index,
                Thread.currentThread().getContextClassLoader(), config);

        return ctx;
    }
}
