package io.automatiko.engine.api.config;

import java.util.Optional;

public class InfinispanPersistenceConfig {

	/**
	 * Infinispan cache template name
	 */
	public Optional<String> template() {
		return Optional.empty();
	}
}
