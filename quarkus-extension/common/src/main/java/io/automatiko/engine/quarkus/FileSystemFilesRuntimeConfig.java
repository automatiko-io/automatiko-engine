package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface FileSystemFilesRuntimeConfig {

    /**
     * Determines the location where files should be stored on file system
     */
    Optional<String> location();

}
