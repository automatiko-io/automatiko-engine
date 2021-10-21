package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AsyncConfig;
import io.automatiko.engine.api.config.AutomatikoConfig;
import io.automatiko.engine.api.config.FilesConfig;
import io.automatiko.engine.api.config.JobsConfig;
import io.automatiko.engine.api.config.MessagingConfig;
import io.automatiko.engine.api.config.MetricsConfig;
import io.automatiko.engine.api.config.PersistenceConfig;
import io.automatiko.engine.api.config.SecurityConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "automatiko", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AutomatikoBuildTimeConfig extends AutomatikoConfig {

    /**
     * Specifies service url that can be used to reach out this service over http
     */
    @ConfigItem
    public Optional<String> serviceUrl;

    /**
     * Specifies package name for generated classes
     */
    @ConfigItem
    public Optional<String> packageName;

    /**
     * Determines if the Automatiko API should be included in OpenAPI definitions, defaults to false
     */
    @ConfigItem
    public Optional<Boolean> includeAutomatikoApi;

    /**
     * Determines if instance locking should be used, defaults to true
     */
    @ConfigItem
    public Optional<Boolean> instanceLocking;

    /**
     * Specifies target deployment that might have impact on generated service
     */
    @ConfigItem
    public Optional<String> targetDeployment;

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
     * Configures metrics
     */
    @ConfigItem
    public MetricsBuildTimeConfig metrics;

    /**
     * Configures messaging for Automatiko
     */
    @ConfigItem
    public MessagingBuildTimeConfig messaging;

    /**
     * Configures persistence
     */
    @ConfigItem
    public PersistenceBuildTimeConfig persistence;

    /**
     * Configures jobs
     */
    @ConfigItem
    public JobsBuildTimeConfig jobs;

    /**
     * Configures security
     */
    @ConfigItem
    public SecurityBuildTimeConfig security;

    /**
     * Configures async subsystem
     */
    @ConfigItem
    public AsyncBuildTimeConfig async;

    /**
     * Configures async subsystem
     */
    @ConfigItem
    public FilesBuildTimeConfig files;

    @Override
    public Optional<String> serviceUrl() {
        return serviceUrl;
    }

    @Override
    public Optional<String> packageName() {
        return packageName;
    }

    @Override
    public Optional<Boolean> includeAutomatikoApi() {
        return includeAutomatikoApi;
    }

    @Override
    public Optional<Boolean> instanceLocking() {
        return instanceLocking;
    }

    @Override
    public Optional<String> targetDeployment() {
        return targetDeployment;
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
    public MetricsConfig metrics() {
        return metrics;
    }

    @Override
    public PersistenceConfig persistence() {
        return persistence;
    }

    @Override
    public MessagingConfig messaging() {
        return messaging;
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

}
