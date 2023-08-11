package io.automatiko.engine.quarkus.function.deployment;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.io.schema.SchemaFactory;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class AutomatikoFunctionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatikoFunctionProcessor.class);

    private static final String FEATURE = "automatiko-function";

    @BuildStep
    FeatureBuildItem generateFunctionFlowDeploymentFiles(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ExampleGenerator generator = new ExampleGenerator();

        AnnotationScannerContext ctx = buildAnnotationScannerContext(index.getIndex());

        Collection<AnnotationInstance> functions = index.getIndex().getAnnotations(createDotName("io.quarkus.funqy.Funq"));

        LOGGER.info("************************************************************");
        LOGGER.info("*************Automatiko Function Instructions***************");
        LOGGER.info("************************************************************");

        // for each found function generate sample payload and print out as instructions
        for (AnnotationInstance f : functions) {
            if (f.target().kind().equals(Kind.METHOD)) {
                MethodInfo mi = f.target().asMethod();
                // create function trigger descriptor for every found function

                SchemaFactory.typeToSchema(ctx,
                        mi.parameters().get(0).type(), null, Collections.emptyList());
                Schema fSchema = ctx.getOpenApi().getComponents().getSchemas()
                        .get(mi.parameters().get(0).type().name().local());

                Map<String, Object> example = generator.generate(fSchema, ctx.getOpenApi());

                LOGGER.info(
                        "Function \"{}\" will accept POST requests on /{} endpoint with following payload ",
                        mi.name(), mi.name());
                Stream.of(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(example).split("\\r?\\n"))
                        .forEach(LOGGER::info);
                LOGGER.info("Alternativelly function accepts GET requests on /{} with following query parameters", mi.name());
                flatMap(null, example).entrySet().forEach(e -> LOGGER.info(e.getKey() + "=" + e.getValue()));
                LOGGER.info("*****************************************");
            }
        }
        LOGGER.info("************************************************************");

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

    @SuppressWarnings("unchecked")
    public static Map<String, String> flatMap(String parentKey, Map<String, Object> nestedMap) {
        Map<String, String> flatMap = new HashMap<>();
        String prefixKey = parentKey != null ? parentKey + "." : "";
        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                flatMap.put(prefixKey + entry.getKey(), (String) entry.getValue());
            }
            if (entry.getValue() instanceof Map) {
                flatMap.putAll(flatMap(prefixKey + entry.getKey(), (Map<String, Object>) entry.getValue()));
            }
        }
        return flatMap;
    }
}
