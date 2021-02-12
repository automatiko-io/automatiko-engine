package io.automatiko.engine.api.config;

import java.util.Optional;

public class ElasticEventsConfig {

    /**
     * Indicates if the audit log events are enabled
     */
    public Optional<Boolean> audit() {
        return Optional.empty();
    }

    /**
     * Indicates if the instance events are enabled
     */
    public Optional<Boolean> instance() {
        return Optional.empty();
    }

    /**
     * Indicates if the user task events are enabled
     */
    public Optional<Boolean> tasks() {
        return Optional.empty();
    }

    /**
     * Determines the name of the audit index in Elastic
     */
    public Optional<String> auditIndex() {
        return Optional.empty();
    }
}
