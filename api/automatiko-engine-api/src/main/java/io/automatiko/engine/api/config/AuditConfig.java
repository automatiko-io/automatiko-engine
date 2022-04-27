package io.automatiko.engine.api.config;

import java.util.Optional;

public class AuditConfig {

    /**
     * Enables auditing, by default it is disabled
     */
    public boolean enabled() {
        return false;
    }

    /**
     * Comma separated types of audit entries that should be included. By default all are included
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     * 
     * @return comma separated list of audit entry types
     */
    public Optional<String> included() {
        return Optional.empty();
    }

    /**
     * Comma separated types of audit entries that should be excluded. By default none are excluded
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     * 
     * @return comma separated list of audit entry types
     */
    public Optional<String> excluded() {
        return Optional.empty();
    }

    /**
     * Configures format of audit entries, available formats are: plain (default) and json
     */
    public Optional<String> format() {
        return Optional.empty();
    }

}
