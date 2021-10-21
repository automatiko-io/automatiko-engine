package io.automatiko.engine.api.config;

import java.util.Optional;

public class S3FilesConfig {

    /**
     * Determines the bucket where files should be stored on AWS S3
     */
    public Optional<String> bucket() {
        return Optional.empty();
    }
}
