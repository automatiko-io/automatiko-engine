package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.PersistenceConfig;
import io.automatiko.engine.api.config.TransactionLogConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PersistenceRuntimeConfig extends PersistenceConfig {

    /**
     * Determines if persistence is enabled
     */
    @ConfigItem
    public Optional<Boolean> disabled;

    /**
     * Determines the type of persistence to be used
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Determines the type of encryption to be used
     */
    @ConfigItem
    public Optional<String> encryption;

    /**
     * Configures file system based persistence
     */
    @ConfigItem
    public FileSystemPersistenceRuntimeConfig filesystem;

    /**
     * Configures database based persistence
     */
    @ConfigItem
    public DatabasePersistenceRuntimeConfig database;

    /**
     * Configures dynamodb based persistence
     */
    @ConfigItem
    public DynamoDBPersistenceRuntimeConfig dynamodb;

    /**
     * Configures cassandra based persistence
     */
    @ConfigItem
    public CassandraPersistenceRuntimeConfig cassandra;

    /**
     * Configures mongodb based persistence
     */
    @ConfigItem
    public MongodbPersistenceRuntimeConfig mongodb;

    /**
     * Configures transaction log
     */
    @ConfigItem
    public TransactionLogRuntimeConfig transactionLog;

    public Optional<Boolean> disabled() {
        return disabled;
    }

    @Override
    public Optional<String> type() {
        return type;
    }

    @Override
    public Optional<String> encryption() {
        return encryption;
    }

    @Override
    public FileSystemPersistenceRuntimeConfig filesystem() {

        return filesystem;
    }

    @Override
    public DatabasePersistenceRuntimeConfig database() {
        return database;
    }

    @Override
    public DynamoDBPersistenceRuntimeConfig dynamodb() {
        return dynamodb;
    }

    @Override
    public CassandraPersistenceRuntimeConfig cassandra() {
        return cassandra;
    }

    @Override
    public MongodbPersistenceRuntimeConfig mongodb() {
        return mongodb;
    }

    @Override
    public TransactionLogConfig transactionLog() {
        return transactionLog;
    }

}
