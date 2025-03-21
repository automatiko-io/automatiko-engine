package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface EventsRuntimeConfig {

    /**
     * Provides configuration of Elastic based events publisher
     */
    ElasticEventsRuntimeConfig elastic();

    /**
     * Provides configuration of websocket based events publisher
     */
    WebsocketEventsRuntimeConfig websocket();

}
