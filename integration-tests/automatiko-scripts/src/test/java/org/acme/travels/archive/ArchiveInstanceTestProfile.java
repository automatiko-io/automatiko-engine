package org.acme.travels.archive;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ArchiveInstanceTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.automatiko.on-instance-end", "archive");
        config.put("quarkus.automatiko.archive-path", new File("target", "archives").getAbsolutePath());
        return config;
    }

}
