package io.automatiko.engine.api.config;

import java.util.Optional;

public class CassandraPersistenceConfig {

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
