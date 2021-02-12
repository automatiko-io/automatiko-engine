package io.automatiko.engine.api.config;

import java.util.Optional;

public class WebsocketEventsConfig {

    /**
     * Indicates if the instance events are enabled
     */
    public Optional<Boolean> instance() {
        return Optional.empty();
    }

    /**
     * Indicates if the user task events are enabled
     */
    public Optional<Boolean> tasks() {
        return Optional.empty();
    }
}
