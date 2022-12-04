package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.GoogleStorageFilesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GoogleStorageFilesRuntimeConfig extends GoogleStorageFilesConfig {

    /**
     * Determines the bucket where files should be stored on Google Storage
     */
    @ConfigItem
    public Optional<String> bucket;

    @Override
    public Optional<String> bucket() {
        return bucket;
    }

}
