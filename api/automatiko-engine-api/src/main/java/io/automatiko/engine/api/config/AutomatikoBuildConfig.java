package io.automatiko.engine.api.config;

import java.util.Optional;

public class AutomatikoBuildConfig {

    public Optional<String> packageName() {
        return Optional.empty();
    };

    public Optional<String> resourcePathPrefix() {
        return Optional.empty();
    };

    public Optional<String> resourcePathFormat() {
        return Optional.empty();
    };

    public Optional<String> sourceFolder() {
        return Optional.empty();
    };

    public Optional<String> projectPaths() {
        return Optional.empty();
    };

    public Optional<Boolean> includeAutomatikoApi() {
        return Optional.empty();
    };

    public Optional<String> targetDeployment() {
        return Optional.empty();
    };

    public MetricsBuildConfig metrics() {
        return new MetricsBuildConfig();
    }

    public PersistenceBuildConfig persistence() {
        return new PersistenceBuildConfig();
    }

    public MessagingBuildConfig messaging() {
        return new MessagingBuildConfig();
    }

    public JobsBuildConfig jobs() {
        return new JobsBuildConfig();
    }

    public RestBuildConfig rest() {
        return new RestBuildConfig();
    }
}
