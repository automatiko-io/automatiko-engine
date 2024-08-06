package io.automatiko.engine.workflow.builder;

import org.eclipse.microprofile.reactive.messaging.Message;

@FunctionalInterface
public interface Correlation<T> {

    /**
     * Extracts identifier that should be used as correlation key (matched against instance id or tags) from the message.
     * Expression that looks either at the payload <code>eventData</code> or message to extract fields or metadata that needs to
     * be of type string.
     * The payload is just a shortcut as <code>message.getPayload()</code> returns same instance. Message also can provide
     * protocal specific data via metadata.
     * 
     * @param eventData payload of the message represented as typed value
     * @param message the message itself
     * @return non nul string to be used as matching criteria against instance id or tags
     */
    public String extract(T eventData, Message<T> message);
}
