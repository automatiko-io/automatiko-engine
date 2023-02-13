package io.automatiko.engine.api.auth;

/**
 * Extension to the <code>AccessPolicy</code> that allow to provide custom implementations
 * that are automatically discovered and registered as AccessPolicies under name given as
 * <code>identifier</code>
 * 
 * @param <T> type of the items the policy apply to
 */
public interface NamedAccessPolicy<T> extends AccessPolicy<T> {

    String identifier();
}
