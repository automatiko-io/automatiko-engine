package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.MongodbPersistenceConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.persistence.mongodb")
public class MongodbPersistenceBuildTimeConfig extends MongodbPersistenceConfig {

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
