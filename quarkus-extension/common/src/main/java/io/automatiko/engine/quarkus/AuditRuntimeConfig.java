package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface AuditRuntimeConfig {

    /**
     * Enables auditing, by default it is disabled
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Comma separated types of audit entries that should be included. By default all are included
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     */
    Optional<String> included();

    /**
     * Comma separated types of audit entries that should be excluded. By default none are excluded
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     */
    Optional<String> excluded();

    /**
     * Configures format of audit entries, available formats are: plain (default) and json
     */
    Optional<String> format();

}
