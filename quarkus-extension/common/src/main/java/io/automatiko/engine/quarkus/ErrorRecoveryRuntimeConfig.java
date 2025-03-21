package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface ErrorRecoveryRuntimeConfig {

    /**
     * Specifies delays for error recovery attempts
     */
    Optional<String> delay();

    /**
     * Specifies comma separated package names (of workflows) to be excluded from error recovery
     */
    Optional<String> excluded();

    /**
     * Specifies comma separated error codes that should be ignored from error recovery
     */
    Optional<String> ignoredErrorCodes();

    /**
     * Specifies maximum number of recovery attempts, defaults to 10
     */
    Optional<Integer> maxIncrementAttempts();

    /**
     * Specifies increment factor in gradually increase the delay between attempts, default to 1.0 meaning it will not be
     * increased.
     * Expected values are from 0.1 to 1.0
     */
    Optional<Double> incrementFactor();

}
