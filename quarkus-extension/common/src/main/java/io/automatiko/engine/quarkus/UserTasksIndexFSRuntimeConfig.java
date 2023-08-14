package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.UserTasksIndexFSConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class UserTasksIndexFSRuntimeConfig extends UserTasksIndexFSConfig {

    /**
     * Provides path where user task index should be stored
     */
    @ConfigItem
    public Optional<String> path;

    @Override
    public Optional<String> path() {
        return path;
    }

}
