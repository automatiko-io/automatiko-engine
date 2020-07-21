
package io.automatik.engine.addons.monitoring.integration;

import java.util.stream.IntStream;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.StringHandler;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringHandlerTest {

	private static final String ENDPOINT_NAME = "hello";
	private static final String DECISION_NAME = "decision";
	CollectorRegistry registry;
	StringHandler handler;

	@BeforeEach
	public void setUp() {
		registry = new CollectorRegistry();
		handler = new StringHandler("hello", registry);
	}

	@AfterEach
	public void destroy() {
		registry.clear();
	}

	@Test
	public void givenSomeStringMetricsWhenMetricsAreStoredThenTheCountIsCorrect() {
		// Arrange
		Double expectedCountStringA = 3.0;
		Double expectedCountStringB = 2.0;
		Double expectedCountStringC = 5.0;

		// Act
		IntStream.rangeClosed(1, 3).forEach(x -> handler.record(DECISION_NAME, ENDPOINT_NAME, "A"));
		IntStream.rangeClosed(1, 2).forEach(x -> handler.record(DECISION_NAME, ENDPOINT_NAME, "B"));
		IntStream.rangeClosed(1, 5).forEach(x -> handler.record(DECISION_NAME, ENDPOINT_NAME, "C"));

		// Assert
		assertEquals(expectedCountStringA, getLabelsValue(DECISION_NAME, ENDPOINT_NAME, "A"));
		assertEquals(expectedCountStringB, getLabelsValue(DECISION_NAME, ENDPOINT_NAME, "B"));
		assertEquals(expectedCountStringC, getLabelsValue(DECISION_NAME, ENDPOINT_NAME, "C"));
	}

	private Double getLabelsValue(String decision, String name, String labelValue) {
		return registry.getSampleValue(name + DecisionConstants.DECISIONS_NAME_SUFFIX,
				DecisionConstants.DECISION_ENDPOINT_IDENTIFIER_LABELS, new String[] { decision, name, labelValue });
	}
}
