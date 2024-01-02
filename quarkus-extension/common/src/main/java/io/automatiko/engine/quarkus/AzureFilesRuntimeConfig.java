package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AzureFilesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AzureFilesRuntimeConfig extends AzureFilesConfig {

    /**
     * Determines the bucket/container where files should be stored on Azure BlobStore
     */
    @ConfigItem
    public Optional<String> bucket;

    @Override
    public Optional<String> bucket() {
        return bucket;
    }

}
