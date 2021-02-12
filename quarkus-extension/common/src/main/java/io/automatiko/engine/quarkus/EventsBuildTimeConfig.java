package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.ElasticEventsConfig;
import io.automatiko.engine.api.config.EventsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class EventsBuildTimeConfig extends EventsConfig {

    /**
     * Provides configuration of Elastic based events publisher
     */
    @ConfigItem
    public ElasticEventsBuildTimeConfig elastic;

    /**
     * Provides configuration of websocket based events publisher
     */
    @ConfigItem
    public WebsocketEventsBuildTimeConfig websocket;

    @Override
    public ElasticEventsConfig elastic() {
        return elastic;
    }

    @Override
    public WebsocketEventsBuildTimeConfig websocket() {
        return websocket;
    }
}
