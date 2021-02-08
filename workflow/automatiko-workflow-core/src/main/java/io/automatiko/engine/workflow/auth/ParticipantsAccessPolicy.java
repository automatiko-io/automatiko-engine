package io.automatiko.engine.workflow.auth;

import java.util.LinkedHashSet;
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

        Set<String> visibleTo = new LinkedHashSet<String>();
        WorkflowProcessInstance pi = (WorkflowProcessInstance) ((AbstractProcessInstance<?>) instance).processInstance();
        if (pi.getInitiator() != null && !pi.getInitiator().isEmpty()) {
            visibleTo.add(pi.getInitiator());
        }
        ((WorkflowProcessInstanceImpl) pi).getNodeInstances(true).stream()
                .filter(ni -> ni instanceof HumanTaskNodeInstance).forEach(ni -> {
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialUsers() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialUsers());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialGroups() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getPotentialGroups());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers());
                    }
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminGroups() != null) {
                        visibleTo.addAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getAdminUsers());
                    }
                    // remove any defined excluded owners
                    if (((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getExcludedUsers() != null) {
                        visibleTo
                                .removeAll(((HumanTaskWorkItem) ((HumanTaskNodeInstance) ni).getWorkItem()).getExcludedUsers());
                    }
                });
        return visibleTo;
    }

}
