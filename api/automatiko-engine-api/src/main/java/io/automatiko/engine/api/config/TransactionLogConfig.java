package io.automatiko.engine.api.config;

import java.util.Optional;

public class TransactionLogConfig {

    /**
     * Controls if the transaction log is enabled, disabled by default
     */
    public Optional<Boolean> enabled() {
        return Optional.empty();
    }

    /**
     * Specifies file system absolute path where transaction log entries should be stored
     */
    public Optional<String> folder() {
        return Optional.empty();
    }
}
