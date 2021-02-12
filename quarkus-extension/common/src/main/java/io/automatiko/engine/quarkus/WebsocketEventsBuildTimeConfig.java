package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.WebsocketEventsConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.events.websocket")
public class WebsocketEventsBuildTimeConfig extends WebsocketEventsConfig {

    /**
     * Indicates if the instance events are enabled
     */
    @ConfigItem
    public Optional<Boolean> instance;

    /**
     * Indicates if the user task events are enabled
     */
    @ConfigItem
    public Optional<Boolean> tasks;

    @Override
    public Optional<Boolean> instance() {
        return instance;
    }

    @Override
    public Optional<Boolean> tasks() {
        return tasks;
    }
}
