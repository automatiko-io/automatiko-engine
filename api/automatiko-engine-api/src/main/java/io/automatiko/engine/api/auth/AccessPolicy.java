package io.automatiko.engine.api.auth;

import java.util.Set;

import io.automatiko.engine.api.workflow.ProcessInstance;

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
    boolean canSignalInstance(IdentityProvider identityProvider, T instance);

    /**
     * Returns currently available users and groups that have read access to the given instance.
     * In case there are no restrictions then this method return null.
     * 
     * @param instance actual instance to apply access policy to
     * @return set of users and groups that can access the given instance
     */
    Set<String> visibleTo(ProcessInstance<?> instance);
}
