package io.automatiko.engine.api.config;

import java.util.Optional;

public class FileSystemPersistenceConfig {

    public static final String PATH_KEY = "quarkus.automatiko.persistence.filesystem.path";
    public static final String LOCK_TIMEOUT_KEY = "quarkus.automatiko.persistence.filesystem.lock-timeout";
    public static final String LOCK_LIMIT_KEY = "quarkus.automatiko.persistence.filesystem.lock-limit";
    public static final String LOCK_WAIT_KEY = "quarkus.automatiko.persistence.filesystem.lock-wait";

    /**
     * File system path to be used as storage location
     */
    public Optional<String> path() {
        return Optional.empty();
    }
}
