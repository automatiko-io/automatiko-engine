package io.automatiko.engine.workflow.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcessInstance;

/**
 * Composite policy that allows to invoke multiple policies in given order
 * 
 * @param <T> type of the items the policy apply to
 */
public class CompositeAccessPolicy<T> implements AccessPolicy<ProcessInstance<T>> {

    private List<AccessPolicy<ProcessInstance<T>>> accessPolicies = new ArrayList<>();

    public CompositeAccessPolicy(String... policies) {

        for (String policy : policies) {
            accessPolicies.add(AccessPolicyFactory.newPolicy(policy.trim()));
        }
    }

    @Override
    public boolean canCreateInstance(IdentityProvider identityProvider) {
        return accessPolicies.stream().allMatch(p -> p.canCreateInstance(identityProvider));
    }

    @Override
    public boolean canReadInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return accessPolicies.stream().allMatch(p -> p.canReadInstance(identityProvider, instance));
    }

    @Override
    public boolean canUpdateInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return accessPolicies.stream().allMatch(p -> p.canUpdateInstance(identityProvider, instance));
    }

    @Override
    public boolean canDeleteInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return accessPolicies.stream().allMatch(p -> p.canDeleteInstance(identityProvider, instance));
    }

    @Override
    public boolean canSignalInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return accessPolicies.stream().allMatch(p -> p.canSignalInstance(identityProvider, instance));
    }

    @Override
    public Set<String> visibleTo(ProcessInstance<?> instance) {

        return ((AbstractProcessInstance<?>) instance).visibleTo();
    }

}
