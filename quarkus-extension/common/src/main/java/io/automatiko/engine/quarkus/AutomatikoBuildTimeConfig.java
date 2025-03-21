package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.automatiko")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface AutomatikoBuildTimeConfig {

    /**
     * Specifies package name for generated classes
     */
    Optional<String> packageName();

    /**
     * Specifies resource path prefix that should be used by REST apis
     */
    Optional<String> resourcePathPrefix();

    /**
     * Specifies resource path format that should be used by REST apis - supported values are:
     * <ul>
     * <li>dash</li>
     * <li>camel</li>
     * </ul>
     * If not set no change will be applied which usually will be camel case format.
     */
    Optional<String> resourcePathFormat();

    /**
     * Specifies source folder in case it is not default maven based project structure
     */
    Optional<String> sourceFolder();

    /**
     * Specifies additional folders where project sources can be found, e.g. when workflows are defined in sub module
     */
    Optional<String> projectPaths();

    /**
     * Determines if the Automatiko API should be included in OpenAPI definitions, defaults to false
     */
    Optional<Boolean> includeAutomatikoApi();

    /**
     * Specifies target deployment that might have impact on generated service
     */
    Optional<String> targetDeployment();

    /**
     * Configures metrics
     */
    MetricsBuildTimeConfig metrics();

    /**
     * Configures messaging for Automatiko
     */
    MessagingBuildTimeConfig messaging();

    /**
     * Configures persistence
     */
    PersistenceBuildTimeConfig persistence();

    /**
     * Configures jobs
     */
    JobsBuildTimeConfig jobs();

    /**
     * Configures rest
     */
    RestBuildTimeConfig rest();

}
