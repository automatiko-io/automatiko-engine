package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.UserTasksIndexConfig;
import io.automatiko.engine.api.config.UserTasksIndexFSConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class UserTasksIndexRuntimeConfig extends UserTasksIndexConfig {

    /**
     * Provides configuration of file system user task index
     */
    @ConfigItem
    public UserTasksIndexFSRuntimeConfig fs;

    @Override
    public UserTasksIndexFSConfig fs() {
        return fs;
    }

}
