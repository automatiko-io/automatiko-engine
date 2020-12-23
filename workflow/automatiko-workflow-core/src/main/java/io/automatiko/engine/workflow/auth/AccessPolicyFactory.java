package io.automatiko.engine.workflow.auth;

import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;

public class AccessPolicyFactory {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> AccessPolicy<ProcessInstance<T>> newPolicy(String identifier) {
        if (identifier == null) {
            return new AllowAllAccessPolicy();
        }
        AccessPolicy<ProcessInstance<T>> policy = null;
        switch (identifier) {
            case "participants":
                policy = new ParticipantsAccessPolicy();
                break;
            case "initiator":
                policy = new InitiatorAccessPolicy();
                break;

            default:
                policy = new AllowAllAccessPolicy();
                break;
        }

        return policy;
    }
}
