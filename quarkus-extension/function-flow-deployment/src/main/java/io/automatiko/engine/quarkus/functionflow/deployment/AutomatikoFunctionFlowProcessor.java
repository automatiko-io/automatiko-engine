package io.automatiko.engine.quarkus.functionflow.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;
import io.automatiko.engine.quarkus.functionflow.deployment.generators.Generator;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScannerContext;

public class AutomatikoFunctionFlowProcessor {

    private static final String FEATURE = "automatiko-function-flow";

    @BuildStep
    FeatureBuildItem generateFunctionFlowDeploymentFiles(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob, AutomatikoBuildTimeConfig config) throws IOException {

        Generator.get(config.targetDeployment().orElse("unknown")).generate(bts, index, cob, config);

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
