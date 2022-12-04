package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.MongodbPersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MongodbPersistenceRuntimeConfig extends MongodbPersistenceConfig {

    /**
     * Name of the data base to be used to create collections for jobs
     */
    @ConfigItem
    public Optional<String> database;

    @Override
    public Optional<String> database() {
        return database;
    }
}
