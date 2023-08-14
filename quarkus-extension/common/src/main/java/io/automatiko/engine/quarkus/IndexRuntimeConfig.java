package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.IndexConfig;
import io.automatiko.engine.api.config.UserTasksIndexConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IndexRuntimeConfig extends IndexConfig {

    /**
     * Provides configuration user tasks index
     */
    @ConfigItem
    public UserTasksIndexRuntimeConfig usertasks;

    @Override
    public UserTasksIndexConfig usertasks() {
        return usertasks;
    }

}
