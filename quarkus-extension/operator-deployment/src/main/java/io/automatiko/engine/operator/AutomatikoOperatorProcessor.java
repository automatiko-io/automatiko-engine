package io.automatiko.engine.operator;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class AutomatikoOperatorProcessor {

    private static final String FEATURE = "automatiko-operator";

    @BuildStep
    FeatureBuildItem automatikoOperatorFeature() {
        return new FeatureBuildItem(FEATURE);
    }
}
