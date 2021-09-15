package io.automatiko.engine.workflow;

import java.util.function.Function;

public class ErrorMapper implements Function<Throwable, Throwable> {

    public Throwable apply(Throwable error) {

        return error;
    }
}