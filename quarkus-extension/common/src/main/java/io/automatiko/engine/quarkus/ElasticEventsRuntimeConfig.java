package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface ElasticEventsRuntimeConfig {

    /**
     * Indicates if the audit log events are enabled
     */
    Optional<Boolean> audit();

    /**
     * Indicates if the instance events are enabled
     */
    Optional<Boolean> instance();

    /**
     * Indicates if the user task events are enabled
     */
    Optional<Boolean> tasks();

    /**
     * Determines the name of the audit index in Elastic
     */
    Optional<String> auditIndex();

}
