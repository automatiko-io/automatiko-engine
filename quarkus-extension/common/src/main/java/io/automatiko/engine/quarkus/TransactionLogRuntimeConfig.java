package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.TransactionLogConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TransactionLogRuntimeConfig extends TransactionLogConfig {

    /**
     * Controls if the transaction log is enabled, disabled by default
     */
    @ConfigItem
    public Optional<Boolean> enabled;

    /**
     * Specifies file system absolute path where transaction log entries should be stored
     */
    @ConfigItem
    public Optional<String> folder;

    @Override
    public Optional<String> folder() {
        return folder;
    }

    @Override
    public Optional<Boolean> enabled() {
        return enabled;
    }

}
