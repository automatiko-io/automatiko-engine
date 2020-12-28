package io.automatiko.engine.api.jobs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
            try {
                return new ExactExpirationTime(LocalDateTime.parse(date).atZone(ZoneId.systemDefault()));
            } catch (DateTimeParseException ex) {
                try {
                    return new ExactExpirationTime(ZonedDateTime.ofInstant(
                            new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy").parse(date).toInstant(),
                            ZoneId.systemDefault()));
                } catch (ParseException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }

    }

    public static ExactExpirationTime now() {
        return new ExactExpirationTime(ZonedDateTime.now());
    }
}
