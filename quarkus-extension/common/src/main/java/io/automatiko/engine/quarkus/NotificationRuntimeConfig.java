package io.automatiko.engine.quarkus;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface NotificationRuntimeConfig {

    /**
     * Allows to globally disable notifications of any type
     */
    Optional<Boolean> disabled();

    /**
     * Provides configuration of email notifications
     */
    Map<String, String> email();

    /**
     * Provides configuration of Slack notifications
     */
    Map<String, String> slack();

    /**
     * Provides configuration of Microsoft Teams notifications
     */
    Map<String, String> teams();

}
