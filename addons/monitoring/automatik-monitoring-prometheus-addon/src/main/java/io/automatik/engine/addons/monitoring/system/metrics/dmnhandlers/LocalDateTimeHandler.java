
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class LocalDateTimeHandler implements TypeHandlerWithSummary<LocalDateTime> {

	private final Summary summary;

	private String dmnType;

	public LocalDateTimeHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public LocalDateTimeHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String type, String endpointName, LocalDateTime sample) {
		summary.labels(type, endpointName).observe(sample.toInstant(ZoneOffset.UTC).toEpochMilli());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}