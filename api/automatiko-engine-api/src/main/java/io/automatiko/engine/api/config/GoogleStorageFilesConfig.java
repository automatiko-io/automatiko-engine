package io.automatiko.engine.api.config;

import java.util.Optional;

public class GoogleStorageFilesConfig {

    /**
     * Determines the bucket where files should be stored on Google Storage
     */
    public Optional<String> bucket() {
        return Optional.empty();
    }
}
