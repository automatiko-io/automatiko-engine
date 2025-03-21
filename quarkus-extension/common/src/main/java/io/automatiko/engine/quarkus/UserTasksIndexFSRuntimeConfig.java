package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface UserTasksIndexFSRuntimeConfig {

    /**
     * Provides path where user task index should be stored
     */
    Optional<String> path();

}
