package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface HttpJobsRuntimeConfig {

    /**
     * File system path to be used as storage location
     */
    Optional<String> url();
}
