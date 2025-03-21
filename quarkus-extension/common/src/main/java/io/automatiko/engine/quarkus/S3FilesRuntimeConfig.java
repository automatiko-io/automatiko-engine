package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface S3FilesRuntimeConfig {

    /**
     * Determines the bucket where files should be stored on AWS S3
     */
    Optional<String> bucket();

}
