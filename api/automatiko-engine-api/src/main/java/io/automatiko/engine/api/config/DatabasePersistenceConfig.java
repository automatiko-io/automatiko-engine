package io.automatiko.engine.api.config;

import java.util.Optional;

public class DatabasePersistenceConfig {

    /**
     * Remove all entities of the model upon completion, defaults to false
     */
    public Optional<Boolean> removeAtCompletion() {
        return Optional.empty();
    }
}
