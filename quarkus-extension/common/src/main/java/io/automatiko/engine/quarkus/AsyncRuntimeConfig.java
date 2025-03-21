package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface AsyncRuntimeConfig {

    /**
     * Provides configuration of async callback
     */
    AsyncCallbackRuntimeConfig callback();

}
