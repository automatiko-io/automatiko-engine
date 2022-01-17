package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.MetricsBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MetricsBuildTimeConfig extends MetricsBuildConfig {

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
