package org.acme.travels.notification;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class WithNotificationsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.automatiko.notifications.slack.test", "http://localhost:8089/webhook-slack");
        config.put("quarkus.automatiko.notifications.teams.test", "http://localhost:8089/webhook-teams");
        return config;
    }

}
