package io.quarkus.funqy.knative.events;

/**
 * THIS IS JUST FOR THE TESTS, ACTUAL IMPL IS IN QUARKUS
 *
 */
public abstract class CloudEventOutput<T> {

    private final String type;

    private final String source;

    private final T data;

    public CloudEventOutput(String type, String source, T data) {
        this.type = type;
        this.source = source;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "CloudEventOutput [type=" + type + ", source=" + source + ", data=" + data + "]";
    }
}
