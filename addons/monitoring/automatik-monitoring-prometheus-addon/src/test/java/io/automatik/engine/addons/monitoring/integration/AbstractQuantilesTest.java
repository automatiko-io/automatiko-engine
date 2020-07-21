
package io.automatik.engine.addons.monitoring.integration;

import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.DecisionConstants;
import io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers.TypeHandler;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

public abstract class AbstractQuantilesTest<T extends TypeHandler> {

	public static final String[] INTERNAL_PROMETHEUS_LABELS = new String[] {
			DecisionConstants.DECISION_ENDPOINT_IDENTIFIER_LABELS[0],
			DecisionConstants.DECISION_ENDPOINT_IDENTIFIER_LABELS[1], "quantile" };
	protected static final String ENDPOINT_NAME = "hello";
	protected CollectorRegistry registry;
	protected T handler;

	protected double getQuantile(String decision, String name, String labelValue, double q) {
		return registry.getSampleValue(name, INTERNAL_PROMETHEUS_LABELS,
				new String[] { decision, labelValue, Collector.doubleToGoString(q) }).doubleValue();
	}
}
