package io.automatiko.engine.service.rest;

import java.util.function.Function;

public class WebErrorMapper implements Function<Throwable, Throwable> {

    @Override
    public Throwable apply(Throwable t) {
        return t;
    }

}
