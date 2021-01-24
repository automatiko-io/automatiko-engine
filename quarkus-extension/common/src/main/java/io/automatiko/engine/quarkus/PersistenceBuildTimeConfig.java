package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.DatabasePersistenceConfig;
import io.automatiko.engine.api.config.DynamoDBPersistenceConfig;
import io.automatiko.engine.api.config.FileSystemPersistenceConfig;
import io.automatiko.engine.api.config.InfinispanPersistenceConfig;
import io.automatiko.engine.api.config.PersistenceConfig;
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

    /**
     * Configures dynamodb based persistence
     */
    @ConfigItem
    public DynamoDBPersistenceBuildTimeConfig dynamodb;

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

    @Override
    public DynamoDBPersistenceConfig dynamodb() {
        return dynamodb;
    }
}
