package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface PersistenceRuntimeConfig {

    /**
     * Determines if persistence is enabled
     */
    Optional<Boolean> disabled();

    /**
     * Determines the type of persistence to be used
     */
    Optional<String> type();

    /**
     * Determines the type of encryption to be used
     */
    Optional<String> encryption();

    /**
     * Configures file system based persistence
     */
    FileSystemPersistenceRuntimeConfig filesystem();

    /**
     * Configures database based persistence
     */
    DatabasePersistenceRuntimeConfig database();

    /**
     * Configures dynamodb based persistence
     */
    DynamoDBPersistenceRuntimeConfig dynamodb();

    /**
     * Configures cassandra based persistence
     */
    CassandraPersistenceRuntimeConfig cassandra();

    /**
     * Configures mongodb based persistence
     */
    MongodbPersistenceRuntimeConfig mongodb();

    /**
     * Configures transaction log
     */
    TransactionLogRuntimeConfig transactionLog();

}
