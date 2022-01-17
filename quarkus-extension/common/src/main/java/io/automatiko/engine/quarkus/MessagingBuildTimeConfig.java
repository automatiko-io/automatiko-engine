package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.MessagingBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MessagingBuildTimeConfig extends MessagingBuildConfig {

	/**
	 * Enables cloud event format for messages
	 */
	@ConfigItem
	public boolean asCloudevents;

	@Override
	public boolean asCloudevents() {
		return asCloudevents;
	}

}
