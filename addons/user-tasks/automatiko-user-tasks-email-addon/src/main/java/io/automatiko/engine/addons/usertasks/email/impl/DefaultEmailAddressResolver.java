package io.automatiko.engine.addons.usertasks.email.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.addons.usertasks.email.EmailAddressResolver;
import io.quarkus.arc.DefaultBean;

@DefaultBean
@ApplicationScoped
public class DefaultEmailAddressResolver implements EmailAddressResolver {

    private static final String EMAIL_REGEX = "^\\s*?(.+)@(.+?)\\s*$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Override
    public Map<String, String> resolve(Collection<String> users, Collection<String> groups) {
        Map<String, String> emails = new HashMap<>();

        if (users != null) {
            users.stream().filter(s -> EMAIL_PATTERN.matcher(s).matches()).forEach(s -> emails.put(s, s));
        }
        if (groups != null) {
            groups.stream().filter(s -> EMAIL_PATTERN.matcher(s).matches()).forEach(s -> emails.put(s, s));
        }

        return emails;
    }

}
