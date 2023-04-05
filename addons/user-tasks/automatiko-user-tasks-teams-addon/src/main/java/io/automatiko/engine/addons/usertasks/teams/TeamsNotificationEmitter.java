package io.automatiko.engine.addons.usertasks.teams;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.usertasks.notification.webhook.WebHookNotificationEmitter;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import io.quarkus.qute.Engine;

@ApplicationScoped
public class TeamsNotificationEmitter extends WebHookNotificationEmitter<TeamsIncomingWebHook> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamsNotificationEmitter.class);

    private static final String DEFAULT_TEMPLATE = "default-usertask-teams";
    private static final String TEMPLATE_SUFFIX = "-teams";

    private static final String WEBHOOK_FOR_CHANNEL_CONFIG_PROPERTY = "quarkus.automatiko.notifications.teams.";

    public TeamsNotificationEmitter() {
        super();
    }

    @Inject
    public TeamsNotificationEmitter(Engine engine,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        super(engine, serviceUrl);
    }

    @Override
    public void notify(LifeCyclePhase phase, Map<String, Object> data, WorkItem workItem) {
        if (isDisabled(workItem)) {
            LOGGER.debug("Notifications are disabled");
            return;
        }

        if (phase.id().equals(Active.ID)) {
            HumanTaskWorkItem humanTask = (HumanTaskWorkItem) workItem;
            String channel = getMetadataValue("teams-channel", workItem);

            if (channel == null || channel.trim().isEmpty()) {
                LOGGER.debug("No slack channel defined for task {} in process {}, ignoring teams notification",
                        humanTask.getParameters().getOrDefault("TaskName",
                                humanTask.getTaskName()),
                        humanTask.getProcessId());
                return;
            }

            TeamsIncomingWebHook webHook = webhook(channel, WEBHOOK_FOR_CHANNEL_CONFIG_PROPERTY);

            if (webHook == null) {
                LOGGER.warn("No slack channel webhook url defined for task {} in process {}, ignoring teams notification",
                        humanTask.getParameters().getOrDefault("TaskName",
                                humanTask.getTaskName()),
                        humanTask.getProcessId());
                return;
            }

            String body = buildTemplate(data, workItem);
            webHook.postMessage(body);
        }
    }

    @Override
    protected String defaultTemplate() {
        return DEFAULT_TEMPLATE;
    }

    @Override
    protected String templateSuffix() {
        return TEMPLATE_SUFFIX;
    }

    @Override
    protected TeamsIncomingWebHook buildConnector(String webhookUrl) {
        return RestClientBuilder.newBuilder().baseUri(URI.create(webhookUrl)).build(TeamsIncomingWebHook.class);
    }

    @Override
    public String toString() {
        return "TeamsNotificationEmitter";
    }
}
