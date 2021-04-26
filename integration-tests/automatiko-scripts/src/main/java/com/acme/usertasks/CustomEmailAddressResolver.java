package com.acme.usertasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.addons.usertasks.email.EmailAddressResolver;

@ApplicationScoped
public class CustomEmailAddressResolver implements EmailAddressResolver {

    @Override
    public Map<String, String> resolve(Collection<String> users, Collection<String> groups) {

        Map<String, String> emails = new HashMap<>();

        return emails;
    }

}
