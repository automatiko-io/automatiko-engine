
package io.automatiko.engine.api.auth;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Delivers security information about given identity that includes name and
 * assigned roles.
 *
 */
public interface IdentityProvider {

    public static final String UNKNOWN_USER_IDENTITY = "unknown";
    static final ThreadLocal<IdentityProvider> current = new ThreadLocal<IdentityProvider>();

    /**
     * Returns name assigned to the current context, usually refers to user name
     * 
     * @return assigned name taken from security context
     */
    String getName();

    /**
     * Returns roles assigned to the current context if any
     * 
     * @return list of assigned roles or empty list
     */
    List<String> getRoles();

    /**
     * Checks if given role is assigned to current context
     * 
     * @param role role to be checked
     * @return true if the role is found otherwise null
     */
    boolean hasRole(String role);

    /**
     * Checks if given identity is an admin
     * 
     * @return returns true if the identity has admin rights otherwise false
     */
    default boolean isAdmin() {
        return hasRole("admin");
    }

    /**
     * Returns map of additional properties that can be consumed by <code>AccessPolicy</code> implementations
     * 
     * @return non null map of properties
     */
    Map<String, Map<String, String>> properties();

    /**
     * Returns currently associated IdentityProvider
     * 
     * @return current identity provider
     */
    static IdentityProvider get() {
        IdentityProvider identity = current.get();

        if (identity == null) {
            return new IdentityProvider() {

                @Override
                public boolean hasRole(String role) {
                    return false;
                }

                @Override
                public List<String> getRoles() {
                    return Collections.emptyList();
                }

                @Override
                public String getName() {
                    return null;
                }

                @Override
                public Map<String, Map<String, String>> properties() {
                    return Collections.emptyMap();
                }
            };
        }

        return identity;
    }

    static void set(IdentityProvider identityProvider) {
        current.set(identityProvider);
    }

    static boolean isSet() {
        return current.get() != null;
    }
}
