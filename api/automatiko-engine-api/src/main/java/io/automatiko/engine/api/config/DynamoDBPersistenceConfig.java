package io.automatiko.engine.api.config;

import java.util.Optional;

public class DynamoDBPersistenceConfig {

    public static final String CREATE_TABLES_KEY = "quarkus.automatiko.persistence.dynamodb.create-tables";
    public static final String READ_CAPACITY_KEY = "quarkus.automatiko.persistence.dynamodb.read-capacity";
    public static final String WRITE_CAPACITY_KEY = "quarkus.automatiko.persistence.dynamodb.write-capacity";

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
