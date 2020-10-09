package io.automatik.engine.quarkus;

import java.util.Optional;

import io.automatik.engine.api.config.DatabasePersistenceConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DatabasePersistenceBuildTimeConfig extends DatabasePersistenceConfig {

    /**
     * Remove all entities of the model upon completion, defaults to false
     */
    @ConfigItem
    public Optional<Boolean> removeAtCompletion;

    public Optional<Boolean> removeAtCompletion() {
        return removeAtCompletion;
    }
}
