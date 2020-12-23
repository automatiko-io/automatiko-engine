package io.automatiko.engine.api.auth;

/**
 * Access policy that drives the access to individual instances based on identity.
 * Enforces if given identity (that is usually representing a user) can access given
 * instance on various levels.
 *
 * @param <T> type of the items the policy apply to
 */
public interface AccessPolicy<T> {

    /**
     * Determines if given identity is allowed to create new instances of item this policy
     * is attached to e.g. creating new process instances
     * 
     * @param identityProvider provider that delivers identity information such as name, roles
     * @return true if given identity is allowed to create new instance
     */
    boolean canCreateInstance(IdentityProvider identityProvider);

    /**
     * Determines if given identity is allowed to read (view) given instance
     * 
     * @param identityProvider provider that delivers identity information such as name, roles
     * @param instance actual instance to apply access policy to
     * @return true if given identity is allowed to read instance
     */
    boolean canReadInstance(IdentityProvider identityProvider, T instance);

    /**
     * Determines if given identity is allowed to update given instance
     * 
     * @param identityProvider provider that delivers identity information such as name, roles
     * @param instance actual instance to apply access policy to
     * @return true if given identity is allowed to update given instance
     */
    boolean canUpdateInstance(IdentityProvider identityProvider, T instance);

    /**
     * Determines if given identity is allowed to delete given instance
     * 
     * @param identityProvider provider that delivers identity information such as name, roles
     * @param instance actual instance to apply access policy to
     * @return true if given identity is allowed to delete given instance
     */
    boolean canDeleteInstance(IdentityProvider identityProvider, T instance);

    /**
     * Determines if given identity is allowed to signal given instance
     * 
     * @param identityProvider provider that delivers identity information such as name, roles
     * @param instance actual instance to apply access policy to
     * @return true if given identity is allowed to signal given instance
     */
    boolean canSignalInstance(IdentityProvider identityProvider, T instances);
}
