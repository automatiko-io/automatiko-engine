package io.automatiko.engine.api.config;

import java.util.Optional;

public class MongodbFilesConfig {

    /**
     * Determines the name of database where files should be stored
     */
    public Optional<String> database() {
        return Optional.empty();
    }

    /**
     * Specifies chunk size (in bytes) to be used when uploading files
     */
    public Optional<Integer> chunkSize() {
        return Optional.empty();
    }
}
