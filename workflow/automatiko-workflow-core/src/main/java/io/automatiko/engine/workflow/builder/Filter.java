package io.automatiko.engine.workflow.builder;

import org.eclipse.microprofile.reactive.messaging.Message;

@FunctionalInterface
public interface Filter<T> {

    public void filter(T eventData, Message<T> message);
}
