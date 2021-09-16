package io.automatiko.engine.workflow.auth;

import java.util.Set;

import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.HumanTaskNodeInstance;

public class ParticipantsAccessPolicy<T> implements AccessPolicy<ProcessInstance<T>> {

    @Override
    public boolean canCreateInstance(IdentityProvider identityProvider) {
        return true;
    }

    @Override
    public boolean canReadInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return whenInitiatorNotSetOrAsIdentity(identityProvider, instance);
    }

    @Override
    public boolean canUpdateInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return whenInitiatorNotSetOrAsIdentity(identityProvider, instance);
    }

    @Override
    public boolean canDeleteInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return whenInitiatorNotSetOrAsIdentity(identityProvider, instance);
    }

    @Override
    public boolean canSignalInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {

        return whenInitiatorNotSetOrAsIdentity(identityProvider, instance);
    }

    protected boolean whenInitiatorNotSetOrAsIdentity(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        if (identityProvider.isAdmin()) {
            return true;
        }

        WorkflowProcessInstance pi = (WorkflowProcessInstance) ((AbstractProcessInstance<?>) instance).processInstance();

        if (pi.getInitiator() == null || pi.getInitiator().isEmpty() || pi.getInitiator().equals(identityProvider.getName())) {
            return true;
        }

        // next check if the user/group is assigned to any of the active user tasks that
        // can make it eligible to access the instance
        return ((WorkflowProcessInstanceImpl) pi).getNodeInstances(true).stream()
                .filter(ni -> ni instanceof HumanTaskNodeInstance).anyMatch(ni -> {
                    HumanTaskWorkItem workitem = (HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem();

                    return workitem.enforce(SecurityPolicy.of(identityProvider));
                });

    }

    @Override
    public Set<String> visibleTo(ProcessInstance<?> instance) {

        return ((AbstractProcessInstance<?>) instance).visibleTo();
    }

}
