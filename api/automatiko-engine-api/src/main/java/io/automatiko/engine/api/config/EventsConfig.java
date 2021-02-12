package io.automatiko.engine.api.config;

public class EventsConfig {

    public ElasticEventsConfig elastic() {
        return new ElasticEventsConfig();
    }

    public WebsocketEventsConfig websocket() {
        return new WebsocketEventsConfig();
    }
}
