package io.automatiko.engine.service.auth;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.NamedAccessPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.auth.AccessPolicyFactory;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class AccessPolicyRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessPolicyRegister.class);

    private Instance<NamedAccessPolicy<ProcessInstance<?>>> accessPolicies;

    public AccessPolicyRegister() {
    }

    @Inject
    public AccessPolicyRegister(Instance<NamedAccessPolicy<ProcessInstance<?>>> accessPolicies) {

        this.accessPolicies = accessPolicies;
    }

    public void registerAvailablePolicies(
            @Observes @Priority(javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE) StartupEvent event) {
        for (NamedAccessPolicy<ProcessInstance<?>> policy : accessPolicies) {
            AccessPolicyFactory.register(policy.identifer(), policy);
            LOGGER.info("Registering access policy {} with identifer '{}'", policy, policy.identifer());
        }
    }

    public void unregisterAvailablePolicies(@Observes ShutdownEvent event) {
        for (NamedAccessPolicy<ProcessInstance<?>> policy : accessPolicies) {
            AccessPolicyFactory.unregister(policy.identifer());
            LOGGER.info("Unregistering access policy with identifer '{}'", policy.identifer());
        }
    }
}
