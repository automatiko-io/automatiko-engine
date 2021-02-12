package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.ElasticEventsConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.events.elastic")
public class ElasticEventsBuildTimeConfig extends ElasticEventsConfig {

    /**
     * Indicates if the audit log events are enabled
     */
    @ConfigItem
    public Optional<Boolean> audit;

    /**
     * Indicates if the instance events are enabled
     */
    @ConfigItem
    public Optional<Boolean> instance;

    /**
     * Indicates if the user task events are enabled
     */
    @ConfigItem
    public Optional<Boolean> tasks;

    /**
     * Determines the name of the audit index in Elastic
     */
    @ConfigItem
    public Optional<String> auditIndex;

    @Override
    public Optional<Boolean> audit() {
        return audit;
    }

    @Override
    public Optional<Boolean> instance() {
        return instance;
    }

    @Override
    public Optional<Boolean> tasks() {
        return tasks;
    }

    @Override
    public Optional<String> auditIndex() {
        return auditIndex;
    }

}
