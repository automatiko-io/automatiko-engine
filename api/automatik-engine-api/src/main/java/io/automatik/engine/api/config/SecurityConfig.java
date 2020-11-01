package io.automatik.engine.api.config;

import java.util.Optional;

public class SecurityConfig {

    /**
     * Configures admin role name used by access policy checks
     * 
     * @return optional name of the admin role to be used, defaults to 'admin'
     */
    public Optional<String> adminRoleName() {
        return Optional.empty();
    }

    /**
     * Configures identity provider build mechanism to either allow to use
     * user and groups given as query parameters or to rely only on
     * security context (authorized requests) defaults to security context only
     * 
     * @return identity provider build mechanism mode
     */
    public boolean authorizedOnly() {
        return true;
    }

}
