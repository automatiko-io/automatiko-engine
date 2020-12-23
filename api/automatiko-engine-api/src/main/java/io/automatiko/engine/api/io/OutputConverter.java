package io.automatiko.engine.api.io;

public interface OutputConverter<V, T> {

    T convert(V value);
}
