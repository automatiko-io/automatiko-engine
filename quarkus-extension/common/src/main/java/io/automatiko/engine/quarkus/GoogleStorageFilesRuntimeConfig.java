package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface GoogleStorageFilesRuntimeConfig {

    /**
     * Determines the bucket where files should be stored on Google Storage
     */
    Optional<String> bucket();

}
