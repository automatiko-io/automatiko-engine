
package io.automatik.engine.workflow;

import io.automatik.engine.api.workflow.Signal;

public final class Sig<T> implements Signal<T> {

    private final String channel;
    private final T payload;
    private String referenceId;

    public static <T> io.automatik.engine.api.workflow.Signal<T> of(String channel, T payload) {
        return new Sig<>(channel, payload);
    }

    public static <T> io.automatik.engine.api.workflow.Signal<T> of(String channel, T payload, String referenceId) {
        return new Sig<>(channel, payload, referenceId);
    }

    public static <T> io.automatik.engine.api.workflow.Signal<T> of(String channel) {
        return new Sig<>(channel, null);
    }

    protected Sig(String channel, T payload) {
        this.channel = channel;
        this.payload = payload;
    }

    protected Sig(String channel, T payload, String referenceId) {
        this.channel = channel;
        this.payload = payload;
        this.referenceId = referenceId;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public T payload() {
        return payload;
    }

    @Override
    public String referenceId() {
        return referenceId;
    }
}
