package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.SecurityConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SecurityBuildTimeConfig extends SecurityConfig {

    /**
     * Configures identity provider build mechanism to either allow to use
     * user and groups given as query parameters or to rely only on
     * security context (authorized requests) defaults to security context only
     * 
     * @return identity provider build mechanism mode
     */
    @ConfigItem(defaultValue = "true")
    public boolean authorizedOnly;

    @Override
    public boolean authorizedOnly() {
        return authorizedOnly;
    }

    /**
     * Configures admin role name used by access policy checks
     * 
     * @return optional name of the admin role to be used, defaults to 'admin'
     */
    @ConfigItem(defaultValue = "admin")
    public Optional<String> adminRoleName;

    @Override
    public Optional<String> adminRoleName() {
        return adminRoleName;
    }

}
