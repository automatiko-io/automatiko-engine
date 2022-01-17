package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.DatabasePersistenceBuildConfig;
import io.automatiko.engine.api.config.PersistenceBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PersistenceBuildTimeConfig extends PersistenceBuildConfig {

    /**
     * Determines the type of persistence to be used
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Configures database based persistence
     */
    @ConfigItem
    public DatabasePersistenceBuildTimeConfig database;

    @Override
    public Optional<String> type() {
        return type;
    }

    @Override
    public DatabasePersistenceBuildConfig database() {
        return database;
    }

}
