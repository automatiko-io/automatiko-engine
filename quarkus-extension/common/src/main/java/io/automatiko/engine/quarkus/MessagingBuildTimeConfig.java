package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface MessagingBuildTimeConfig {

    /**
     * Enables cloud event format for messages
     */
    @WithDefault("false")
    boolean asCloudevents();

    /**
     * Instructs to use binary binding mode for cloud event for messages
     */
    @WithDefault("false")
    boolean asCloudeventsBinary();

}
