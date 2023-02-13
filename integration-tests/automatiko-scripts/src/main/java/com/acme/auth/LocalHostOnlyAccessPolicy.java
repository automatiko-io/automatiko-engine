package com.acme.auth;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.NamedAccessPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.vertx.core.http.HttpServerRequest;

@ApplicationScoped
public class LocalHostOnlyAccessPolicy<T> implements NamedAccessPolicy<ProcessInstance<T>> {

    @Inject
    HttpServerRequest injectedHttpServletRequest;

    private boolean allowFromLocalhostOnly() {

        if (injectedHttpServletRequest != null && injectedHttpServletRequest.host() != null
                && !injectedHttpServletRequest.host().toLowerCase().startsWith("localhost")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canCreateInstance(IdentityProvider identityProvider) {
        return allowFromLocalhostOnly();
    }

    @Override
    public boolean canReadInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return allowFromLocalhostOnly();
    }

    @Override
    public boolean canUpdateInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return allowFromLocalhostOnly();
    }

    @Override
    public boolean canDeleteInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return allowFromLocalhostOnly();
    }

    @Override
    public boolean canSignalInstance(IdentityProvider identityProvider, ProcessInstance<T> instance) {
        return allowFromLocalhostOnly();
    }

    @Override
    public Set<String> visibleTo(ProcessInstance<?> instance) {

        return ((AbstractProcessInstance<?>) instance).visibleTo();
    }

    @Override
    public String identifer() {
        return "localHostOnly";
    }

}
