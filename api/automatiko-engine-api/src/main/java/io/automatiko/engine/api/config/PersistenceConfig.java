package io.automatiko.engine.api.config;

import java.util.Optional;

public class PersistenceConfig {

    /**
     * Determines the type of persistence to be used
     */
    public Optional<String> type() {
        return Optional.empty();
    }

    /**
     * Configures file system based persistence
     */
    public FileSystemPersistenceConfig filesystem() {
        return new FileSystemPersistenceConfig();
    }

    /**
     * Configures infinispan based persistence
     */
    public InfinispanPersistenceConfig infinispan() {
        return new InfinispanPersistenceConfig();
    }

    /**
     * Configures database based persistence
     */
    public DatabasePersistenceConfig database() {
        return new DatabasePersistenceConfig();
    }

    /**
     * Configures dynamodb based persistence
     */
    public DynamoDBPersistenceConfig dynamodb() {
        return new DynamoDBPersistenceConfig();
    }

    /**
     * Configures cassandra based persistence
     */
    public CassandraPersistenceConfig cassandra() {
        return new CassandraPersistenceConfig();
    }
}
