package io.automatiko.engine.api.config;

import java.util.Optional;

public class AutomatikoConfig {

    public Optional<String> serviceUrl() {
        return Optional.empty();
    };

    public Optional<Boolean> serviceRouteToLatest() {
        return Optional.empty();
    };

    public Optional<Boolean> instanceLocking() {
        return Optional.of(true);
    };

    public Optional<String> onInstanceEnd() {
        return Optional.empty();
    };

    public Optional<String> archivePath() {
        return Optional.empty();
    };

    public Optional<String> templatesFolder() {
        return Optional.empty();
    };

    public PersistenceConfig persistence() {
        return new PersistenceConfig() {
        };
    }

    public JobsConfig jobs() {
        return new JobsConfig() {
        };
    }

    public SecurityConfig security() {
        return new SecurityConfig() {
        };
    }

    public AsyncConfig async() {
        return new AsyncConfig() {
        };
    }

    public FilesConfig files() {
        return new FilesConfig() {
        };
    }

    public AuditConfig audit() {
        return new AuditConfig();
    }
}
