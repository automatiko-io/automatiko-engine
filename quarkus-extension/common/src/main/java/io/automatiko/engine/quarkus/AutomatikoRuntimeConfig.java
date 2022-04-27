package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AsyncConfig;
import io.automatiko.engine.api.config.AuditConfig;
import io.automatiko.engine.api.config.AutomatikoConfig;
import io.automatiko.engine.api.config.FilesConfig;
import io.automatiko.engine.api.config.JobsConfig;
import io.automatiko.engine.api.config.PersistenceConfig;
import io.automatiko.engine.api.config.SecurityConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "automatiko", phase = ConfigPhase.RUN_TIME)
public class AutomatikoRuntimeConfig extends AutomatikoConfig {

    /**
     * Specifies service url that can be used to reach out this service over http
     */
    @ConfigItem
    public Optional<String> serviceUrl;

    /**
     * Determines if instance locking should be used, defaults to true
     */
    @ConfigItem
    public Optional<Boolean> instanceLocking;

    /**
     * Specifies strategy to be applied on end of the workflow instance. Defaults to remove instance at the end of
     * its life time. Alternatively it can be set to keep the instance or archive the instance.
     */
    @ConfigItem
    public Optional<String> onInstanceEnd;

    /**
     * Specifies location on the file system where to store archived process instance when using
     * <code>onInstanceEnd</code> set to <code>archive</code> with default file system based archive store
     */
    @ConfigItem
    public Optional<String> archivePath;

    /**
     * Configures persistence
     */
    @ConfigItem
    public PersistenceRuntimeConfig persistence;

    /**
     * Configures jobs
     */
    @ConfigItem
    public JobsRuntimeConfig jobs;

    /**
     * Configures security
     */
    @ConfigItem
    public SecurityRuntimeConfig security;

    /**
     * Configures async subsystem
     */
    @ConfigItem
    public AsyncRuntimeConfig async;

    /**
     * Configures files subsystem
     */
    @ConfigItem
    public FilesRuntimeConfig files;

    /**
     * Configures auditing support
     */
    @ConfigItem
    public AuditRuntimeConfig audit;

    @Override
    public Optional<String> serviceUrl() {
        return serviceUrl;
    }

    @Override
    public Optional<Boolean> instanceLocking() {
        return instanceLocking;
    }

    @Override
    public Optional<String> onInstanceEnd() {
        return onInstanceEnd;
    }

    @Override
    public Optional<String> archivePath() {
        return archivePath;
    }

    @Override
    public PersistenceConfig persistence() {
        return persistence;
    }

    @Override
    public JobsConfig jobs() {
        return jobs;
    }

    @Override
    public SecurityConfig security() {
        return security;
    }

    @Override
    public AsyncConfig async() {
        return async;
    }

    @Override
    public FilesConfig files() {
        return files;
    }

    @Override
    public AuditConfig audit() {
        return audit;
    }
}
