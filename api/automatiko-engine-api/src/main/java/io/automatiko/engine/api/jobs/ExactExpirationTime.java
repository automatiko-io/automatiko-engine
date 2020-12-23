package io.automatiko.engine.api.jobs;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class ExactExpirationTime implements ExpirationTime {

    private final ZonedDateTime expirationTime;

    private ExactExpirationTime(ZonedDateTime expirationTime) {
        this.expirationTime = Objects.requireNonNull(expirationTime);
    }

    @Override
    public ZonedDateTime get() {
        return expirationTime;
    }

    @Override
    public Long repeatInterval() {
        return null;
    }

    @Override
    public Integer repeatLimit() {
        return 0;
    }

    public static ExactExpirationTime of(ZonedDateTime expirationTime) {
        return new ExactExpirationTime(expirationTime);
    }

    public static ExactExpirationTime of(String date) {
        try {
            return new ExactExpirationTime(ZonedDateTime.parse(date));
        } catch (DateTimeParseException e) {
            return new ExactExpirationTime(LocalDateTime.parse(date).atZone(ZoneId.systemDefault()));
        }

    }

    public static ExactExpirationTime now() {
        return new ExactExpirationTime(ZonedDateTime.now());
    }
}
