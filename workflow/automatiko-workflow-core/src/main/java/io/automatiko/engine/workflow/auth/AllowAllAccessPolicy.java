package io.automatiko.engine.workflow.auth;

import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.workflow.ProcessInstance;

/**
 * Access policy that does not have any restrictions
 */
public class AllowAllAccessPolicy<T> implements AccessPolicy<ProcessInstance<T>> {

    @Override
    public boolean canCreateInstance(IdentityProvider identityProvider) {
        return true;
    }

    @Override
    public boolean canReadInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return true;
    }

    @Override
    public boolean canUpdateInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return true;
    }

    @Override
    public boolean canDeleteInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return true;
    }

    @Override
    public boolean canSignalInstance(IdentityProvider identityProvider, ProcessInstance<T> instances) {
        return true;
    }

}
