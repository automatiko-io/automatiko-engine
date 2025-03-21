package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DatabasePersistenceBuildTimeConfig {

    /**
     * Remove all entities of the model upon completion, defaults to false
     */
    Optional<Boolean> removeAtCompletion();

}
