
package io.automatik.engine.addons.monitoring.integration;

import java.time.LocalDate;
import java.time.ZoneOffset;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.LocalDateHandler;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalDateHandlerTest extends AbstractQuantilesTest<LocalDateHandler> {

	@BeforeEach
	public void setUp() {
		registry = new CollectorRegistry();
		handler = new LocalDateHandler("hello", registry);
	}

	@AfterEach
	public void destroy() {
		registry.clear();
	}

	@Test
	public void givenLocalDateMetricsWhenMetricsAreStoredThenTheQuantilesAreCorrect() {
		// Arrange
		LocalDate now = LocalDate.now();
		Long expectedValue = now.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
		Double[] quantiles = new Double[] { 0.1, 0.25, 0.5, 0.75, 0.9, 0.99 };

		// Act
		handler.record("decision", ENDPOINT_NAME, now);

		// Assert
		for (Double key : quantiles) {
			assertEquals(expectedValue, getQuantile("decision", ENDPOINT_NAME + DecisionConstants.DECISIONS_NAME_SUFFIX,
					ENDPOINT_NAME, key), 5);
		}
	}
}