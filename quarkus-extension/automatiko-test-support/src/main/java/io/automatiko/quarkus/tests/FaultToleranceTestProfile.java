package io.automatiko.quarkus.tests;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class FaultToleranceTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.arc.selected-alternatives",
                "io.automatiko.addons.fault.tolerance.internal.AutomatikoStrategyCache");
        return config;
    }

}
