package io.automatiko.engine.api.config;

import java.util.Optional;

public class PersistenceBuildConfig {

    /**
     * Determines the type of persistence to be used
     */
    public Optional<String> type() {
        return Optional.empty();
    }

    /**
     * Configures database based persistence
     */
    public DatabasePersistenceBuildConfig database() {
        return new DatabasePersistenceBuildConfig();
    }

}
