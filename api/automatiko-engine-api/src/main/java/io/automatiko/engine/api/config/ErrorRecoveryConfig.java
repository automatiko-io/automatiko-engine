package io.automatiko.engine.api.config;

import java.util.Optional;

public class ErrorRecoveryConfig {

    public Optional<String> delay() {
        return Optional.empty();
    };

    public Optional<String> excluded() {
        return Optional.empty();
    };

    public Optional<String> ignoredErrorCodes() {
        return Optional.empty();
    };

    public Optional<Integer> maxIncrementAttempts() {
        return Optional.empty();
    };

    public Optional<Double> incrementFactor() {
        return Optional.empty();
    };
}
