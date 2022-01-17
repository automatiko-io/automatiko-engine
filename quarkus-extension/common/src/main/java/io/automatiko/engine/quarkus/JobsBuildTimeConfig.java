package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.JobsBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JobsBuildTimeConfig extends JobsBuildConfig {

    /**
     * Determines the type of persistence to be used
     */
    @ConfigItem
    public Optional<String> type;

    @Override
    public Optional<String> type() {
        return type;
    }

}
