
package io.automatiko.engine.services.event;

import io.automatiko.engine.api.event.AbstractDataEvent;

public abstract class AbstractProcessDataEvent<T> extends AbstractDataEvent<T> {

    public AbstractProcessDataEvent(String source, T body) {
        this(null, source, body);
    }

    public AbstractProcessDataEvent(String type, String source, T body) {
        super(type, source, body);
    }

    public AbstractProcessDataEvent(String specversion, String id, String source, String type, String time, T data) {
        super(specversion, id, source, type, null, time, data);
    }
}
