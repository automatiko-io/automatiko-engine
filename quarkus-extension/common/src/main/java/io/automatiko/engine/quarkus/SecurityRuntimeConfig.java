package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface SecurityRuntimeConfig {

    /**
     * Configures identity provider build mechanism to either allow to use
     * user and groups given as query parameters or to rely only on
     * security context (authorized requests) defaults to security context only
     * 
     * @return identity provider build mechanism mode
     */
    @WithDefault("true")
    boolean authorizedOnly();

    /**
     * Configures admin role name used by access policy checks
     * 
     * @return optional name of the admin role to be used, defaults to 'admin'
     */
    @WithDefault("admin")
    Optional<String> adminRoleName();

}
