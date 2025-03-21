package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface AzureFilesRuntimeConfig {

    /**
     * Determines the bucket/container where files should be stored on Azure BlobStore
     */
    Optional<String> bucket();

}
