
package io.automatik.engine.addons.monitoring.integration;

import java.time.Period;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.YearsAndMonthsDurationHandler;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YearsAndMonthsDurationHandlerTest extends AbstractQuantilesTest<YearsAndMonthsDurationHandler> {

	@BeforeEach
	public void setUp() {
		registry = new CollectorRegistry();
		handler = new YearsAndMonthsDurationHandler("hello", registry);
	}

	@AfterEach
	public void destroy() {
		registry.clear();
	}

	@Test
	public void givenYearsAndMonthsMetricsWhenMetricsAreStoredThenTheQuantilesAreCorrect() {
		// Arrange
		Integer expectedValue = 12;
		Period period = Period.ofMonths(expectedValue);
		Double[] quantiles = new Double[] { 0.1, 0.25, 0.5, 0.75, 0.9, 0.99 };

		// Act
		handler.record("decision", ENDPOINT_NAME, period);

		// Assert
		for (Double key : quantiles) {
			assertEquals(expectedValue, getQuantile("decision", ENDPOINT_NAME + DecisionConstants.DECISIONS_NAME_SUFFIX,
					ENDPOINT_NAME, key), 5);
		}
	}
}
