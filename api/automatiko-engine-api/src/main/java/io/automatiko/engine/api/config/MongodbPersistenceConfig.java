package io.automatiko.engine.api.config;

import java.util.Optional;

public class MongodbPersistenceConfig {

    public static final String DATABASE_KEY = "quarkus.automatiko.persistence.mongodb.database";

    /**
     * Name of the data base to be used to create collections for processes
     */
    public Optional<String> database() {
        return Optional.empty();
    }
}
