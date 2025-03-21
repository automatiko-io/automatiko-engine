package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface IndexRuntimeConfig {

    /**
     * Provides configuration user tasks index
     */
    UserTasksIndexRuntimeConfig usertasks();

}
