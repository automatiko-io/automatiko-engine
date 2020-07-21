
package io.automatik.engine.addons.monitoring.system.metrics.dmnhandlers;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;

public interface TypeHandlerWithSummary<T> extends TypeHandler<T> {
	default Summary initializeDefaultSummary(String dmnType, CollectorRegistry registry) {
		Summary.Builder builder = Summary.build() // Calculate quantiles over a sliding window of time - default = 10
													// minutes
				.quantile(0.1, 0.01) // Add 10th percentile with 1% tolerated error
				.quantile(0.25, 0.05).quantile(0.50, 0.05) // Add 50th percentile (= median) with 5% tolerated error
				.quantile(0.75, 0.05).quantile(0.9, 0.05).quantile(0.99, 0.01)
				.name(dmnType.replace(" ", "_") + DecisionConstants.DECISIONS_NAME_SUFFIX)
				.help(DecisionConstants.DECISIONS_HELP).labelNames(DecisionConstants.DECISION_ENDPOINT_LABELS);
		return registry == null ? builder.register(CollectorRegistry.defaultRegistry) : builder.register(registry);
	}
}
