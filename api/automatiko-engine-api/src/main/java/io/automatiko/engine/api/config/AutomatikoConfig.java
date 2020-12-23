package io.automatiko.engine.api.config;

import java.util.Optional;

public class AutomatikoConfig {

    public Optional<String> serviceUrl() {
        return Optional.empty();
    };

    public Optional<String> packageName() {
        return Optional.empty();
    };

    public Optional<Boolean> includeAutomatikoApi() {
        return Optional.empty();
    };

    public MetricsConfig metrics() {
        return new MetricsConfig();
    }

    public PersistenceConfig persistence() {
        return new PersistenceConfig();
    }

    public MessagingConfig messaging() {
        return new MessagingConfig();
    }

    public JobsConfig jobs() {
        return new JobsConfig();
    }

    public SecurityConfig security() {
        return new SecurityConfig();
    }
}
