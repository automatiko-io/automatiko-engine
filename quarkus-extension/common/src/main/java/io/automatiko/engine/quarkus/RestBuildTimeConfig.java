package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface RestBuildTimeConfig {

    /**
     * Enables REST for automatiko
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Specifies which HTTP method should be used for update variables endpoint - POST or PUT defaults to POST
     */
    @WithDefault("POST")
    Optional<String> updateDataMethod();

}
