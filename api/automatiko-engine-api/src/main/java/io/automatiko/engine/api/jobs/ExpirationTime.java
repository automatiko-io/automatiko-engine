package io.automatiko.engine.api.jobs;

import java.time.ZonedDateTime;

public interface ExpirationTime {

    ZonedDateTime get();

    default ZonedDateTime next() {
        return null;
    }

    Long repeatInterval();

    Integer repeatLimit();

    default String expression() {
        return null;
    }

}
