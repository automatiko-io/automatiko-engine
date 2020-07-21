
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.time.LocalTime;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class LocalTimeHandler implements TypeHandlerWithSummary<LocalTime> {

	private final Summary summary;

	private String dmnType;

	public LocalTimeHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public LocalTimeHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String type, String endpointName, LocalTime sample) {
		summary.labels(type, endpointName).observe(sample.toSecondOfDay());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}
