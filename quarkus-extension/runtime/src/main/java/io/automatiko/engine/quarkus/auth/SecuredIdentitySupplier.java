package io.automatiko.engine.quarkus.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class SecuredIdentitySupplier implements IdentitySupplier {

    @Inject
    Instance<SecurityIdentity> securityInstance;

    @ConfigProperty(name = "quarkus.automatiko.security.authorized-only")
    Optional<Boolean> authroizedOnly;

    @ConfigProperty(name = "quarkus.automatiko.security.admin-role-name")
    Optional<String> adminRoleName;

    public IdentityProvider buildIdentityProvider(String user, List<String> roles) {

        if (IdentityProvider.isSet()) {
            return IdentityProvider.get();
        }
        Principal principal = retrievePrincipal();
        if (securityInstance.isUnsatisfied() || principal == null) {

            StaticIdentityProvider current = new StaticIdentityProvider(adminRoleName.orElse("admin"), user, roles);

            IdentityProvider.set(current);
            return current;
        }

        String principalName = principal.getName();
        if (!authroizedOnly.orElse(true) && user != null) {
            principalName = user;
        }
        StaticIdentityProvider current = new StaticIdentityProvider(adminRoleName.orElse("admin"), principalName,
                mergeRoles(securityInstance.get(), roles));

        IdentityProvider.set(current);
        return current;
    }

    protected List<String> mergeRoles(SecurityIdentity securityIdentity, List<String> roles) {
        if (!authroizedOnly.orElse(true) && roles != null) {
            // add given roles only when "authroizedOnly" is not strict meaning does rely on authorized context only
            ArrayList<String> merged = new ArrayList<String>(securityIdentity.getRoles());
            merged.addAll(roles);
            return merged;

        }

        return new ArrayList<String>(securityIdentity.getRoles());
    }

    protected Principal retrievePrincipal() {
        if (securityInstance.isUnsatisfied()) {
            return null;
        }
        try {
            return securityInstance.get().getPrincipal();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
