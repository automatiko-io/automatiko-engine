package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface CassandraPersistenceRuntimeConfig {

    /**
     * Indicates if keyspace should be created during startup
     */
    Optional<Boolean> createKeyspace();

    /**
     * Indicates if tables should be created during startup
     */
    Optional<Boolean> createTables();

    /**
     * Specifies if keyspace name to be used if not given it defaults to application name
     */
    Optional<String> keyspace();

}
