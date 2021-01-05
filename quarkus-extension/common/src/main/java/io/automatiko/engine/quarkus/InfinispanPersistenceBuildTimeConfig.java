package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.InfinispanPersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class InfinispanPersistenceBuildTimeConfig extends InfinispanPersistenceConfig {

	/**
	 * Infinispan cache template name
	 */
	@ConfigItem
	public Optional<String> template;

	@Override
	public Optional<String> template() {
		return template;
	}
}
