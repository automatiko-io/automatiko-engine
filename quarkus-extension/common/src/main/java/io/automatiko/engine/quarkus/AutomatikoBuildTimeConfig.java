package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AutomatikoConfig;
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
     * Configures jobs
     */
    @ConfigItem
    public SecurityBuildTimeConfig security;

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

}
