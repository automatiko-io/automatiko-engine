package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface MongodbPersistenceRuntimeConfig {

    /**
     * Name of the data base to be used to create collections for jobs
     */
    Optional<String> database();
}
