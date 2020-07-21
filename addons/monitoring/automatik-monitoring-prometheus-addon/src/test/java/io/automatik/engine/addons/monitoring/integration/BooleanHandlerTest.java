
package io.automatik.engine.addons.monitoring.integration;

import java.util.stream.IntStream;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.BooleanHandler;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooleanHandlerTest {

	private static final String ENDPOINT_NAME = "hello";

	CollectorRegistry registry;
	BooleanHandler handler;

	@BeforeEach
	public void setUp() {
		registry = new CollectorRegistry();
		handler = new BooleanHandler("hello", registry);
	}

	@AfterEach
	public void destroy() {
		registry.clear();
	}

	@Test
	public void givenSomeBooleanMetricsWhenMetricsAreStoredThenTheCountIsCorrect() {
		// Arrange
		Double expectedTrue = 3.0;
		Double expectedFalse = 2.0;

		// Act
		IntStream.rangeClosed(1, 3).forEach(x -> handler.record("decision", ENDPOINT_NAME, true));
		IntStream.rangeClosed(1, 2).forEach(x -> handler.record("decision", ENDPOINT_NAME, false));

		// Assert
		assertEquals(expectedTrue, getLabelsValue("decision", ENDPOINT_NAME, "true"));
		assertEquals(expectedFalse, getLabelsValue("decision", ENDPOINT_NAME, "false"));
	}

	private Double getLabelsValue(String decision, String name, String labelValue) {
		return registry.getSampleValue(name + DecisionConstants.DECISIONS_NAME_SUFFIX,
				DecisionConstants.DECISION_ENDPOINT_IDENTIFIER_LABELS, new String[] { decision, name, labelValue });
	}
}
