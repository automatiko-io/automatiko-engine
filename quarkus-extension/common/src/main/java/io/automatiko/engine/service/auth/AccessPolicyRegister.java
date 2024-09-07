package io.automatiko.engine.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.NamedAccessPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.auth.AccessPolicyFactory;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
            @Observes @Priority(jakarta.interceptor.Interceptor.Priority.APPLICATION - 1) StartupEvent event) {
        for (NamedAccessPolicy<ProcessInstance<?>> policy : accessPolicies) {
            AccessPolicyFactory.register(policy.identifier(), policy);
            LOGGER.info("Registering access policy {} with identifer '{}'", policy, policy.identifier());
        }
    }

    public void unregisterAvailablePolicies(@Observes ShutdownEvent event) {
        for (NamedAccessPolicy<ProcessInstance<?>> policy : accessPolicies) {
            AccessPolicyFactory.unregister(policy.identifier());
            LOGGER.info("Unregistering access policy with identifer '{}'", policy.identifier());
        }
    }
}
