package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.CassandraPersistenceConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.persistence.cassandra")
public class CassandraPersistenceBuildTimeConfig extends CassandraPersistenceConfig {

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
    public Optional<Boolean> createTables() {
        return createTables;
    }

    @Override
    public Optional<String> keyspace() {
        return keyspace;
    }

}
