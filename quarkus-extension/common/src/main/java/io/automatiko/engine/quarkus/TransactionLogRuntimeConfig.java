package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface TransactionLogRuntimeConfig {

    /**
     * Controls if the transaction log is enabled, disabled by default
     */
    Optional<Boolean> enabled();

    /**
     * Specifies file system absolute path where transaction log entries should be stored
     */
    Optional<String> folder();

}
