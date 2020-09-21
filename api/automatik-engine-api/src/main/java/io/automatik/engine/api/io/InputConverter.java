package io.automatik.engine.api.io;

public interface InputConverter<T> {

    T convert(Object input);
}
