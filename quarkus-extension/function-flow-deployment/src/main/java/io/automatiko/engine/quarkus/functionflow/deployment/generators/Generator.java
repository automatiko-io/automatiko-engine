package io.automatiko.engine.quarkus.functionflow.deployment.generators;

import java.io.IOException;

import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public interface Generator {

    void generate(BuildSystemTargetBuildItem bts, CombinedIndexBuildItem index,
            CurateOutcomeBuildItem cob, AutomatikoBuildTimeConfig config) throws IOException;

    public static Generator get(String deploymentTarget) {
        if (deploymentTarget.equals("gcp-pubsub")) {
            return new GcpPubSubGenerator();
        }

        return new DefaultGenerator();
    }
}
