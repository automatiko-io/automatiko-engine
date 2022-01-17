package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.DynamoDBPersistenceConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.persistence.dynamodb")
public class DynamoDBPersistenceRuntimeConfig extends DynamoDBPersistenceConfig {

    /**
     * Indicates if tables should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createTables;

    /**
     * Specifies the read capacity, if not set defaults to 10
     */
    @ConfigItem
    public Optional<Long> readCapacity;

    /**
     * Specifies the write capacity, if not set defaults to 10
     */
    @ConfigItem
    public Optional<Long> writeCapacity;

    @Override
    public Optional<Boolean> createTables() {
        return createTables;
    }

    @Override
    public Optional<Long> readCapacity() {
        return readCapacity;
    }

    @Override
    public Optional<Long> writeCapacity() {
        return writeCapacity;
    }
}
