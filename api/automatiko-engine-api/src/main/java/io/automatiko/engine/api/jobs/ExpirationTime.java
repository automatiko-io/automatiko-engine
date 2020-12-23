package io.automatiko.engine.api.jobs;

import java.time.ZonedDateTime;

public interface ExpirationTime {

	ZonedDateTime get();

	Long repeatInterval();

	Integer repeatLimit();
}
