package io.automatik.engine.quarkus;

import io.automatik.engine.api.config.MetricsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MetricsBuildTimeConfig extends MetricsConfig {

	/**
	 * Enables metrics for automatik
	 */
	@ConfigItem(defaultValue = "false")
	public boolean enabled;

	@Override
	public boolean enabled() {
		return enabled;
	}
}
