package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface UserTasksIndexRuntimeConfig {

    /**
     * Provides configuration of file system user task index
     */
    UserTasksIndexFSRuntimeConfig fs();

}
