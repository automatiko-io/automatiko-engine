package io.automatiko.engine.addons.usertasks.email;

import java.util.Collection;
import java.util.Map;

public interface EmailAddressResolver {

    /**
     * Resolves email addresses for given users and groups.
     * 
     * @param users list of users to get email addresses for
     * @param groups list of groups to get email addresses for
     * @return complete map of email addresses resolved for given users and groups where the key is the user/group and the value
     *         is email address
     */
    Map<String, String> resolve(Collection<String> users, Collection<String> groups);
}
