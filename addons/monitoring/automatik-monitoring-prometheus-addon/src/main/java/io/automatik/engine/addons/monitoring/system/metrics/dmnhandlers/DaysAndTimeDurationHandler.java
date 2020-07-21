
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.time.Duration;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class DaysAndTimeDurationHandler implements TypeHandlerWithSummary<Duration> {

	private final Summary summary;

	private String dmnType;

	public DaysAndTimeDurationHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public DaysAndTimeDurationHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String type, String endpointName, Duration sample) {
		summary.labels(type, endpointName).observe(sample.toMillis());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}