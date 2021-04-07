package io.automatiko.engine.workflow.base.core.timer;

import java.time.ZonedDateTime;

import io.automatiko.engine.api.jobs.ExpirationTime;

public class NoOpExpirationTime implements ExpirationTime {

    @Override
    public ZonedDateTime get() {
        return null;
    }

    @Override
    public Long repeatInterval() {
        return null;
    }

    @Override
    public Integer repeatLimit() {
        return 0;
    }

}
