package io.automatik.engine.api.config;

import java.util.Optional;

public class PersistenceConfig {

	/**
	 * Determines the type of persistence to be used
	 */
	public Optional<String> type() {
		return Optional.empty();
	}

	/**
	 * Configures file system based persistence
	 */
	public FileSystemPersistenceConfig filesystem() {
		return new FileSystemPersistenceConfig();
	}

	/**
	 * Configures infinispan based persistence
	 */
	public InfinispanPersistenceConfig infinispan() {
		return new InfinispanPersistenceConfig();
	}
}
