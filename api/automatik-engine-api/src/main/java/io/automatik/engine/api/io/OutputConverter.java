package io.automatik.engine.api.io;

public interface OutputConverter<V, T> {

    T convert(V value);
}
