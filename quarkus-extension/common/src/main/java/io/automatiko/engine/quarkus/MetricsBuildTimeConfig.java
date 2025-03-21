package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface MetricsBuildTimeConfig {

    /**
     * Enables metrics for automatik
     */
    @WithDefault("false")
    boolean enabled();

}
