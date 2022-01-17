package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.DatabasePersistenceBuildConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DatabasePersistenceBuildTimeConfig extends DatabasePersistenceBuildConfig {

    /**
     * Remove all entities of the model upon completion, defaults to false
     */
    @ConfigItem
    public Optional<Boolean> removeAtCompletion;

    public Optional<Boolean> removeAtCompletion() {
        return removeAtCompletion;
    }
}
