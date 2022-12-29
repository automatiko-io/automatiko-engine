package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.ErrorRecoveryConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ErrorRecoveryRuntimeConfig extends ErrorRecoveryConfig {

    /**
     * Specifies delays for error recovery attempts
     */
    @ConfigItem
    public Optional<String> delay;

    /**
     * Specifies comma separated package names (of workflows) to be excluded from error recovery
     */
    @ConfigItem
    public Optional<String> excluded;

    /**
     * Specifies comma separated error codes that should be ignored from error recovery
     */
    @ConfigItem
    public Optional<String> ignoredErrorCodes;

    /**
     * Specifies maximum number of recovery attempts, defaults to 10
     */
    @ConfigItem
    public Optional<Integer> maxIncrementAttempts;

    /**
     * Specifies increment factor in gradually increase the delay between attempts, default to 1.0 meaning it will not be
     * increased.
     * Expected values are from 0.1 to 1.0
     */
    @ConfigItem
    public Optional<Double> incrementFactor;

    @Override
    public Optional<String> delay() {
        return delay;
    }

    @Override
    public Optional<String> excluded() {
        return excluded;
    }

    @Override
    public Optional<String> ignoredErrorCodes() {
        return ignoredErrorCodes;
    }

    @Override
    public Optional<Integer> maxIncrementAttempts() {
        return maxIncrementAttempts;
    }

    @Override
    public Optional<Double> incrementFactor() {
        return incrementFactor;
    }

}
