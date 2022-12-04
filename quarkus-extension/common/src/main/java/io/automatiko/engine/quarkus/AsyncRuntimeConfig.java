package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.AsyncCallbackConfig;
import io.automatiko.engine.api.config.AsyncConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AsyncRuntimeConfig extends AsyncConfig {

    /**
     * Provides configuration of async callback
     */
    @ConfigItem
    public AsyncCallbackRuntimeConfig callback;

    @Override
    public AsyncCallbackConfig callback() {
        return callback;
    }
}
