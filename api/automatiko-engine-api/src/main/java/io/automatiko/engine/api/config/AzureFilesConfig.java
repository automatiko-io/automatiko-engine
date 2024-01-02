package io.automatiko.engine.api.config;

import java.util.Optional;

public class AzureFilesConfig {

    /**
     * Determines the bucket/container where files should be stored on Azure BlobStore
     */
    public Optional<String> bucket() {
        return Optional.empty();
    }
}
