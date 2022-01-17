package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.FileSystemJobsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FileSystemJobsRuntimeConfig extends FileSystemJobsConfig {

    /**
     * File system path to be used as storage location
     */
    @ConfigItem
    public Optional<String> path;

    /**
     * Number of threads to be used for jobs execution
     */
    @ConfigItem
    public Optional<Integer> threads;

    @Override
    public Optional<String> path() {
        return path;
    }

    @Override
    public Optional<Integer> threads() {
        return threads;
    }
}
