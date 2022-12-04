package io.automatiko.engine.api.config;

import java.util.Optional;

public class ElasticEventsConfig {
    public static final String AUDIT_KEY = "quarkus.automatiko.events.elastic.audit";
    public static final String INSTANCE_KEY = "quarkus.automatiko.events.elastic.instance";
    public static final String TASKS_KEY = "quarkus.automatiko.events.elastic.tasks";
    public static final String AUDIT_INDEX_KEY = "quarkus.automatiko.events.elastic.audit-index";

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
