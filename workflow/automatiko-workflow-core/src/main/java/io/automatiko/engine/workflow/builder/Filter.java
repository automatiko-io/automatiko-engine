package io.automatiko.engine.workflow.builder;

import org.eclipse.microprofile.reactive.messaging.Message;

@FunctionalInterface
public interface Filter<T> {

    /**
     * Filter expression that looks either at the payload <code>eventData</code> or message to verify if it should be
     * accepted or rejected for processing. The payload is just a shortcut as <code>message.getPayload()</code> returns
     * same instance. Message also can provide protocal specific data via metadata.
     * 
     * @param eventData payload of the message represented as typed value
     * @param message the message itself
     * @return true if message should be processed/consumed otherwise false
     */
    public boolean filter(T eventData, Message<T> message);
}
