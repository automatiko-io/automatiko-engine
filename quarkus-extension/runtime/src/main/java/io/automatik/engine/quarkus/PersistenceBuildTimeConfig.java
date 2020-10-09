package io.automatik.engine.quarkus;

import java.util.Optional;

import io.automatik.engine.api.config.DatabasePersistenceConfig;
import io.automatik.engine.api.config.FileSystemPersistenceConfig;
import io.automatik.engine.api.config.InfinispanPersistenceConfig;
import io.automatik.engine.api.config.PersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PersistenceBuildTimeConfig extends PersistenceConfig {

    /**
     * Determines the type of persistence to be used
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Configures file system based persistence
     */
    @ConfigItem
    public FileSystemPersistenceBuildTimeConfig filesystem;

    /**
     * Configures infinispan based persistence
     */
    @ConfigItem
    public InfinispanPersistenceBuildTimeConfig infinispan;

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
    public FileSystemPersistenceConfig filesystem() {

        return filesystem;
    }

    @Override
    public InfinispanPersistenceConfig infinispan() {
        return infinispan;
    }

    @Override
    public DatabasePersistenceConfig database() {
        return database;
    }
}
