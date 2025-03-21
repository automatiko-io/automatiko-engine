package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface FileSystemJobsRuntimeConfig {

    /**
     * File system path to be used as storage location
     */
    Optional<String> path();

    /**
     * Number of threads to be used for jobs execution
     */
    Optional<Integer> threads();

}
