package org.acme.travels;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class StructuredCloudEventTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.automatiko.messaging.as-cloudevents", "true");
        config.put("quarkus.automatiko.messaging.as-cloudevents-binary", "true");
        return config;
    }

}
