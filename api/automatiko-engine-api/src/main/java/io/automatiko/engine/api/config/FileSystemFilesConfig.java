package io.automatiko.engine.api.config;

import java.util.Optional;

public class FileSystemFilesConfig {

    /**
     * Determines the location where files should be stored on file system
     */
    public Optional<String> location() {
        return Optional.empty();
    }
}
