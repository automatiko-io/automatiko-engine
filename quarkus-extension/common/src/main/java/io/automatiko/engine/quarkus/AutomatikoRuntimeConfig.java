package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.automatiko")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AutomatikoRuntimeConfig {

    /**
     * Specifies service url that can be used to reach out this service over http
     */
    Optional<String> serviceUrl();

    /**
     * Specifies if there should be routing to latest version enabled, applies to ReST service interface
     * and when versions are used. It is by default turned off
     */
    Optional<Boolean> serviceRouteToLatest();

    /**
     * Determines if instance locking should be used, defaults to true
     */
    @WithDefault("true")
    Optional<Boolean> instanceLocking();

    /**
     * Specifies strategy to be applied on end of the workflow instance. Defaults to remove instance at the end of
     * its life time. Alternatively it can be set to keep the instance or archive the instance.
     */
    Optional<String> onInstanceEnd();

    /**
     * Specifies location on the file system where to store archived process instance when using
     * <code>onInstanceEnd</code> set to <code>archive</code> with default file system based archive store
     */
    Optional<String> archivePath();

    /**
     * Specifies templates folder to populate for customized email and user task templates
     */
    Optional<String> templatesFolder();

    /**
     * Configures persistence
     */
    PersistenceRuntimeConfig persistence();

    /**
     * Configures jobs
     */
    JobsRuntimeConfig jobs();

    /**
     * Configures security
     */
    SecurityRuntimeConfig security();

    /**
     * Configures async subsystem
     */
    AsyncRuntimeConfig async();

    /**
     * Configures files subsystem
     */
    FilesRuntimeConfig files();

    /**
     * Configures auditing support
     */
    AuditRuntimeConfig audit();

    /**
     * Configures error recovery support
     */
    ErrorRecoveryRuntimeConfig errorRecovery();

    /**
     * Configures notification support
     */
    NotificationRuntimeConfig notifications();

    /**
     * Configures index support
     */
    IndexRuntimeConfig index();
}
