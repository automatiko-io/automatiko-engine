package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.RestBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestBuildTimeConfig extends RestBuildConfig {

    /**
     * Enables REST for automatiko
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Specifies which HTTP method should be used for update variables endpoint - POST or PUT defaults to POST
     */
    @ConfigItem(defaultValue = "POST")
    public Optional<String> updateDataMethod;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public Optional<String> updateDataMethod() {
        return updateDataMethod;
    }
}
