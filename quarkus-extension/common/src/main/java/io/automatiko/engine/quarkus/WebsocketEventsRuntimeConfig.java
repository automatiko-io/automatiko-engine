package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface WebsocketEventsRuntimeConfig {

    /**
     * Indicates if the instance events are enabled
     */
    Optional<Boolean> instance();

    /**
     * Indicates if the user task events are enabled
     */
    Optional<Boolean> tasks();

}
