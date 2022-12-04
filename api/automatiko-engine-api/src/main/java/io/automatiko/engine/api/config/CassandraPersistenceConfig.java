package io.automatiko.engine.api.config;

import java.util.Optional;

public class CassandraPersistenceConfig {

    public static final String CREATE_KEYSPACE_KEY = "quarkus.automatiko.persistence.cassandra.create-keyspace";
    public static final String CREATE_TABLES_KEY = "quarkus.automatiko.persistence.cassandra.create-tables";
    public static final String KEYSPACE_KEY = "quarkus.automatiko.persistence.cassandra.keyspace";

    /**
     * Indicates if keyspace should be created during startup.
     */
    public Optional<Boolean> createKeyspace() {
        return Optional.empty();
    }

    /**
     * Indicates if tables should be created during startup.
     */
    public Optional<Boolean> createTables() {
        return Optional.empty();
    }

    /**
     * Specifies if keyspace name to be used if not given it defaults to 'automatiko'
     */
    public Optional<String> keyspace() {
        return Optional.empty();
    }
}
