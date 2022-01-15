
package io.automatiko.engine.api.event;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This is an abstract implementation of the {@link DataEvent} that contains
 * basic common attributes referring to automatiko processes metadata. This class
 * can be extended mainly by Services that need to publish events to be indexed
 * by the Data-Index service.
 *
 * @param <T> the payload
 */
public abstract class AbstractDataEvent<T> implements DataEvent<T> {

    protected String specversion;
    protected String id;
    protected String source;
    protected String type;
    protected String time;
    protected String datacontenttype;
    protected T data;

    protected Map<String, Object> extensions = new HashMap<>();

    public AbstractDataEvent(String type, String source, T body) {
        this.specversion = SPEC_VERSION;
        this.id = UUID.randomUUID().toString();
        this.source = source;
        this.type = type;
        this.time = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        this.datacontenttype = "application/json";
        this.data = body;
    }

    public AbstractDataEvent(String specversion, String id, String source, String type, String time, T data) {
        this.specversion = specversion;
        this.id = id;
        this.source = source;
        this.type = type;
        this.time = time == null ? ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : time;
        this.datacontenttype = "application/json";
        this.data = data;
    }

    public AbstractDataEvent(String specversion, String id, String source, String type, String time, String datacontenttype,
            T data) {
        this.specversion = specversion;
        this.id = id;
        this.source = source;
        this.type = type;
        this.time = time;
        this.datacontenttype = datacontenttype;
        this.data = data;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getSpecversion() {
        return specversion;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTime() {
        return time;
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        this.extensions.put(name, value);
    }

    @Override
    public Object getExtension(String name) {
        return this.extensions.get(name);
    }
}
