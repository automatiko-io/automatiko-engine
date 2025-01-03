package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AutomatikoBuildConfig;
import io.automatiko.engine.api.config.JobsBuildConfig;
import io.automatiko.engine.api.config.MessagingBuildConfig;
import io.automatiko.engine.api.config.MetricsBuildConfig;
import io.automatiko.engine.api.config.PersistenceBuildConfig;
import io.automatiko.engine.api.config.RestBuildConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "automatiko", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AutomatikoBuildTimeConfig extends AutomatikoBuildConfig {

    /**
     * Specifies package name for generated classes
     */
    @ConfigItem
    public Optional<String> packageName;

    /**
     * Specifies resource path prefix that should be used by REST apis
     */
    @ConfigItem
    public Optional<String> resourcePathPrefix;

    /**
     * Specifies resource path format that should be used by REST apis - supported values are:
     * <ul>
     * <li>dash</li>
     * <li>camel</li>
     * </ul>
     * If not set no change will be applied which usually will be camel case format.
     */
    @ConfigItem
    public Optional<String> resourcePathFormat;

    /**
     * Specifies source folder in case it is not default maven based project structure
     */
    @ConfigItem
    public Optional<String> sourceFolder;

    /**
     * Specifies additional folders where project sources can be found, e.g. when workflows are defined in sub module
     */
    @ConfigItem
    public Optional<String> projectPaths;

    /**
     * Determines if the Automatiko API should be included in OpenAPI definitions, defaults to false
     */
    @ConfigItem
    public Optional<Boolean> includeAutomatikoApi;

    /**
     * Specifies target deployment that might have impact on generated service
     */
    @ConfigItem
    public Optional<String> targetDeployment;

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
     * Configures rest
     */
    @ConfigItem
    public RestBuildTimeConfig rest;

    public Optional<String> packageName() {
        return packageName;
    }

    public Optional<String> resourcePathPrefix() {
        return resourcePathPrefix;
    };

    public Optional<String> resourcePathFormat() {
        return resourcePathFormat;
    };

    public Optional<String> sourceFolder() {
        return sourceFolder;
    };

    public Optional<String> projectPaths() {
        return projectPaths;
    };

    public Optional<Boolean> includeAutomatikoApi() {
        return includeAutomatikoApi;
    }

    public Optional<String> targetDeployment() {
        return targetDeployment;
    }

    public MetricsBuildConfig metrics() {
        return metrics;
    }

    public PersistenceBuildConfig persistence() {
        return persistence;
    }

    public MessagingBuildConfig messaging() {
        return messaging;
    }

    public JobsBuildConfig jobs() {
        return jobs;
    }

    @Override
    public RestBuildConfig rest() {
        return rest;
    }

}
