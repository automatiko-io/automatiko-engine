package io.automatiko.engine.workflow.auth;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;

public class AccessPolicyFactory {

    private static final Map<String, AccessPolicy<?>> REGISTERED = new HashMap<>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> AccessPolicy<ProcessInstance<T>> newPolicy(String identifier) {
        if (identifier == null) {
            return new AllowAllAccessPolicy();
        }
        String[] options = new String[0];
        if (identifier.contains(":")) {
            String[] idWithOptions = identifier.split(":");
            identifier = idWithOptions[0];
            options = idWithOptions[1].split(",");
        }

        AccessPolicy<ProcessInstance<T>> policy = (AccessPolicy<ProcessInstance<T>>) REGISTERED.get(identifier);

        if (policy == null) {
            switch (identifier.toLowerCase()) {
                case "participants":
                    policy = new ParticipantsAccessPolicy();
                    break;
                case "initiator":
                    policy = new InitiatorAccessPolicy();
                    break;
                case "composite":
                    policy = new CompositeAccessPolicy(options);
                    break;

                default:
                    policy = null;
                    break;
            }
        }
        if (policy == null) {
            throw new IllegalArgumentException("No access policy was found with identifier '" + identifier + "'");
        }
        return policy;
    }

    public static void register(String identifier, AccessPolicy<?> policy) {
        if (REGISTERED.containsKey(identifier)) {
            throw new IllegalStateException("Not possible to register access policy under " + identifier
                    + " as there is already one registered " + REGISTERED.get(identifier));
        }

        REGISTERED.put(identifier, policy);
    }

    public static void unregister(String identifer) {
        REGISTERED.remove(identifer);

    }
}
