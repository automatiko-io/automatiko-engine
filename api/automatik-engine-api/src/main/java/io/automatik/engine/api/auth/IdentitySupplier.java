package io.automatik.engine.api.auth;

import java.util.List;

/**
 * IdentitySupplier is meant to provide a way to inject/configure IdentityProviders to be used by the application.
 */
public interface IdentitySupplier {

    /**
     * Creates new identity provider based on optionally given user and roles.
     * Depending on the implementation actual security context can be injected and thus used.
     * 
     * @param user optional user name to be used
     * @param roles optional additional roles to be used
     * @return configured identity provider instance
     */
    IdentityProvider buildIdentityProvider(String user, List<String> roles);
}
