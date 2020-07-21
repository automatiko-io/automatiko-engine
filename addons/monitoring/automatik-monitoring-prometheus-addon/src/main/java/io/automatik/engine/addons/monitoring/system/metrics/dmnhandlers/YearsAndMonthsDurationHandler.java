
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import java.time.Period;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public class YearsAndMonthsDurationHandler implements TypeHandlerWithSummary<Period> {

	private final Summary summary;

	private String dmnType;

	public YearsAndMonthsDurationHandler(String dmnType, CollectorRegistry registry) {
		this.dmnType = dmnType;
		this.summary = initializeDefaultSummary(dmnType, registry);
	}

	public YearsAndMonthsDurationHandler(String dmnType) {
		this(dmnType, null);
	}

	@Override
	public void record(String type, String endpointName, Period sample) {
		summary.labels(type, endpointName).observe(sample.toTotalMonths());
	}

	@Override
	public String getDmnType() {
		return dmnType;
	}
}
