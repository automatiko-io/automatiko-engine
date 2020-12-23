package io.automatiko.engine.api.config;

import java.util.Optional;

public class FileSystemJobsConfig {

	/**
	 * File system path to be used as storage location
	 */
	public Optional<String> path() {
		return Optional.empty();
	}

	/**
	 * Number of threads to be used for jobs execution
	 */
	public Optional<Integer> threads() {
		return Optional.empty();
	}
}
