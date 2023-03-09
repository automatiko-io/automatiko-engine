package io.automatiko.engine.api.config;

import java.util.Map;
import java.util.Optional;

public class NotificationsConfig {

    public Optional<Boolean> disabled;

    public Map<String, String> email() {
        return null;
    }

    public Map<String, String> slack() {
        return null;
    }

    public Map<String, String> teams() {
        return null;
    }
}
