
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.time.LocalDate;
import java.time.ZoneOffset;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class LocalDateHandler implements TypeHandlerWithSummary<LocalDate> {

	private final Summary summary;

	private String dmnType;

	public LocalDateHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public LocalDateHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String type, String endpointName, LocalDate sample) {
		summary.labels(type, endpointName).observe(sample.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}
