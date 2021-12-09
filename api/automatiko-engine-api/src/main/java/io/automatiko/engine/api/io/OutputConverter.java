package io.automatiko.engine.api.io;

import io.automatiko.engine.api.runtime.process.ProcessInstance;

public interface OutputConverter<V, T> {

    T convert(V value);

    default <M> M metadata(ProcessInstance pi, Class<M> metadataClass) {
        return null;
    }
}
