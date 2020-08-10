package io.automatik.engine.quarkus;

import java.util.Optional;

import io.automatik.engine.api.config.FileSystemJobsConfig;
import io.automatik.engine.api.config.HttpJobsConfig;
import io.automatik.engine.api.config.JobsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JobsBuildTimeConfig extends JobsConfig {

	/**
	 * Determines the type of persistence to be used
	 */
	@ConfigItem
	public Optional<String> type;

	/**
	 * Configures jobs based on file system storage
	 */
	@ConfigItem
	public FileSystemJobsBuildTimeConfig filesystem;

	/**
	 * Configures jobs based on file system storage
	 */
	@ConfigItem
	public HttpJobsBuildTimeConfig http;

	@Override
	public Optional<String> type() {
		return type;
	}

	@Override
	public FileSystemJobsConfig filesystem() {
		return filesystem;
	}

	@Override
	public HttpJobsConfig http() {
		return http;
	}

}
