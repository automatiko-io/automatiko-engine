package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.FileSystemFilesConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.files.fs")
public class FileSystemFilesRuntimeConfig extends FileSystemFilesConfig {

    /**
     * Determines the location where files should be stored on file system
     */
    @ConfigItem
    public Optional<String> location;

    @Override
    public Optional<String> location() {
        return location;
    }

}
