
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.math.BigDecimal;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class BigDecimalHandler implements TypeHandlerWithSummary<BigDecimal> {

	private final Summary summary;

	private String dmnType;

	public BigDecimalHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public BigDecimalHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String decision, String endpointName, BigDecimal sample) {
		summary.labels(decision, endpointName).observe(sample.doubleValue());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}
