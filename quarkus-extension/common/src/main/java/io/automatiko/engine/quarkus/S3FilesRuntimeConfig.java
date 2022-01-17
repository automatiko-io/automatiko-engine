package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.S3FilesConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.files.s3")
public class S3FilesRuntimeConfig extends S3FilesConfig {

    /**
     * Determines the bucket where files should be stored on AWS S3
     */
    @ConfigItem
    public Optional<String> bucket;

    @Override
    public Optional<String> bucket() {
        return bucket;
    }

}
