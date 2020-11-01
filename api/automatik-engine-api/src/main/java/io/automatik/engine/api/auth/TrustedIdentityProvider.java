package io.automatik.engine.api.auth;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Trusted identity provider is intended to be used by system wide operations
 * that might not have any security context attached e.g. timer expiration,
 * messaging consumers etc.
 *
 * Trusted identity is considered an admin so it can perform any operation
 */
public class TrustedIdentityProvider implements IdentityProvider {

    private final String name;

    public TrustedIdentityProvider(String name) {
        this.name = name;
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    @Override
    public String getName() {
        // returns null on purpose to avoid setting this as initiator
        return null;
    }

    @Override
    public List<String> getRoles() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasRole(String role) {
        return true;
    }

    @Override
    public String toString() {
        return "TrustedIdentityProvider [name=" + name + "]";
    }

    @Override
    public Map<String, Map<String, String>> properties() {
        return Collections.emptyMap();
    }

}
