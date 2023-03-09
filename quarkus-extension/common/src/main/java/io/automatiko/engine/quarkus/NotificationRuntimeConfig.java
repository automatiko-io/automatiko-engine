package io.automatiko.engine.quarkus;

import java.util.Map;
import java.util.Optional;

import io.automatiko.engine.api.config.NotificationsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class NotificationRuntimeConfig extends NotificationsConfig {

    /**
     * Allows to globally disable notifications of any type
     */
    @ConfigItem
    public Optional<Boolean> disabled;

    /**
     * Provides configuration of email notifications
     */
    @ConfigItem
    public Map<String, String> email;

    /**
     * Provides configuration of Slack notifications
     */
    @ConfigItem
    public Map<String, String> slack;

    /**
     * Provides configuration of Microsoft Teams notifications
     */
    @ConfigItem
    public Map<String, String> teams;

    @Override
    public Map<String, String> email() {
        return email;
    }

    @Override
    public Map<String, String> slack() {
        return slack;
    }

    @Override
    public Map<String, String> teams() {
        return teams;
    }

}
