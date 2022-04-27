package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AuditConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.audit")
public class AuditRuntimeConfig extends AuditConfig {

    /**
     * Enables auditing, by default it is disabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    @Override
    public boolean enabled() {
        return enabled;
    }

    /**
     * Comma separated types of audit entries that should be included. By default all are included
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     */
    @ConfigItem
    public Optional<String> included;

    @Override
    public Optional<String> included() {
        return included;
    }

    /**
     * Comma separated types of audit entries that should be excluded. By default none are excluded
     * 
     * Supported types are:
     * workflow,workflow_node,workflow_variable,workflow_persistence_read,workflow_persistence_write,messaging,timer
     */
    @ConfigItem
    public Optional<String> excluded;

    @Override
    public Optional<String> excluded() {
        return excluded;
    }

    /**
     * Configures format of audit entries, available formats are: plain (default) and json
     */
    @ConfigItem
    public Optional<String> format;

    @Override
    public Optional<String> format() {
        return format;
    }
}
