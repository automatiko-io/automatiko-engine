package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.FileSystemPersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FileSystemPersistenceRuntimeConfig extends FileSystemPersistenceConfig {

	/**
	 * File system path to be used as storage location
	 */
	@ConfigItem
	public Optional<String> path;

	@Override
	public Optional<String> path() {
		return path;
	}
}
