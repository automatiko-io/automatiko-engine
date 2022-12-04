package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.CassandraPersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CassandraPersistenceRuntimeConfig extends CassandraPersistenceConfig {

    /**
     * Indicates if keyspace should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createKeyspace;

    /**
     * Indicates if tables should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createTables;

    /**
     * Specifies if keyspace name to be used if not given it defaults to application name
     */
    @ConfigItem
    public Optional<String> keyspace;

    @Override
    public Optional<Boolean> createKeyspace() {
        return createKeyspace;
    }

    @Override
    public Optional<Boolean> createTables() {
        return createTables;
    }

    @Override
    public Optional<String> keyspace() {
        return keyspace;
    }

}
