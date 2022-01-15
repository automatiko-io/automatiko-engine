package io.automatiko.engine.api.config;

public class MessagingBuildConfig {

    /**
     * Enables cloud event format for messages
     */
    public boolean asCloudevents() {
        return true;
    }

    /**
     * Instructs to use binary binding mode for cloud event for messages
     */
    public boolean asCloudeventsBinary() {
        return true;
    }
}
