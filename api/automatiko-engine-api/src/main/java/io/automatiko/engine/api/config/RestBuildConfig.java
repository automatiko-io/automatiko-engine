package io.automatiko.engine.api.config;

import java.util.Optional;

public class RestBuildConfig {

    /**
     * Enables REST for automatiko
     */
    public boolean enabled() {
        return false;
    }

    public Optional<String> updateDataMethod() {
        return Optional.of("POST");
    }
}
