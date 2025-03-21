package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface JobsBuildTimeConfig {

    /**
     * Determines the type of persistence to be used
     */
    Optional<String> type();

}
