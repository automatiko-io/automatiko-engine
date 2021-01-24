package io.automatiko.engine.api.config;

import java.util.Optional;

public class DynamoDBPersistenceConfig {

    /**
     * Indicates if tables should be created during startup.
     */
    public Optional<Boolean> createTables() {
        return Optional.empty();
    }

    /**
     * Specifies the read capacity
     */
    public Optional<Long> readCapacity() {
        return Optional.empty();
    }

    /**
     * Specifies the write capacity
     */
    public Optional<Long> writeCapacity() {
        return Optional.empty();
    }
}
