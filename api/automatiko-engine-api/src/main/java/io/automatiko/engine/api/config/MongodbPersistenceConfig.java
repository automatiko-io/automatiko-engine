package io.automatiko.engine.api.config;

import java.util.Optional;

public class MongodbPersistenceConfig {

    public static final String DATABASE_KEY = "quarkus.automatiko.persistence.mongodb.database";
    public static final String LOCK_TIMEOUT_KEY = "quarkus.automatiko.persistence.mongodb.lock-timeout";
    public static final String LOCK_LIMIT_KEY = "quarkus.automatiko.persistence.mongodb.lock-limit";
    public static final String LOCK_WAIT_KEY = "quarkus.automatiko.persistence.mongodb.lock-wait";

    /**
     * Name of the data base to be used to create collections for processes
     */
    public Optional<String> database() {
        return Optional.empty();
    }
}
