package io.automatiko.engine.api.io;

public interface InputConverter<T> {

    T convert(Object input);
}
