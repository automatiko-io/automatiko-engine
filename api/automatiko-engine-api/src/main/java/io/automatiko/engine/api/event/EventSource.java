package io.automatiko.engine.api.event;

public interface EventSource {

    void produce(String type, String source, Object data);
}
