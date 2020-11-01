
package io.automatik.engine.services.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.auth.IdentityProvider;

/**
 * Simple implementation of identity provider that must always be used for just
 * one security context, needs to be recreated every time for each "request"
 *
 * Relies on given name and roles
 */
public class StaticIdentityProvider implements IdentityProvider {

    private String adminRoleName = "admin";

    private String name;
    private List<String> roles;
    private Map<String, Map<String, String>> properties;

    public StaticIdentityProvider(String name) {
        this(name, Collections.emptyList());
    }

    public StaticIdentityProvider(String name, List<String> roles) {
        this(name, roles, Collections.emptyMap());
    }

    public StaticIdentityProvider(String name, List<String> roles, Map<String, Map<String, String>> properties) {
        this.name = name;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.properties = properties;
    }

    public StaticIdentityProvider(String adminRoleName, String name) {
        this(name, Collections.emptyList());
    }

    public StaticIdentityProvider(String adminRoleName, String name, List<String> roles) {
        this(name, roles, Collections.emptyMap());
    }

    public StaticIdentityProvider(String adminRoleName, String name, List<String> roles,
            Map<String, Map<String, String>> properties) {
        this.adminRoleName = adminRoleName;
        this.name = name;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public void addProperties(Map<String, Map<String, String>> props) {
        this.properties.putAll(props);
    }

    @Override
    public Map<String, Map<String, String>> properties() {
        return properties;
    }

    @Override
    public boolean isAdmin() {
        return hasRole(adminRoleName);
    }

}
