
package io.automatik.engine.addons.events.rm;

import java.util.Collection;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import io.automatik.engine.api.event.DataEvent;
import io.automatik.engine.api.event.EventPublisher;

@Singleton
public class ReactiveMessagingEventPublisher implements EventPublisher {
    private static final String PI_TOPIC_NAME = "kogito-processinstances-events";
    private static final String UI_TOPIC_NAME = "kogito-usertaskinstances-events";
    private static final String VI_TOPIC_NAME = "kogito-variables-events";

    private static final Logger logger = LoggerFactory.getLogger(ReactiveMessagingEventPublisher.class);
    private ObjectMapper json = new ObjectMapper();

    @Inject
    @Channel(PI_TOPIC_NAME)
    Emitter<String> processInstancesEventsEmitter;

    @Inject
    @Channel(UI_TOPIC_NAME)
    Emitter<String> userTasksEventsEmitter;

    @Inject
    @Channel(VI_TOPIC_NAME)
    Emitter<String> variablesEventsEmitter;

    @Inject
    @ConfigProperty(name = "kogito.events.processinstances.enabled")
    Optional<Boolean> processInstancesEvents;

    @Inject
    @ConfigProperty(name = "kogito.events.usertasks.enabled")
    Optional<Boolean> userTasksEvents;

    @Inject
    @ConfigProperty(name = "kogito.events.variables.enabled")
    Optional<Boolean> variablesEvents;

    @PostConstruct
    public void configure() {
        json.setDateFormat(new StdDateFormat().withColonInTimeZone(true).withTimeZone(TimeZone.getDefault()));
    }

    @Override
    public void publish(DataEvent<?> event) {
        if (event.getType().equals("ProcessInstanceEvent") && processInstancesEvents.orElse(true)) {

            publishToTopic(event, processInstancesEventsEmitter, PI_TOPIC_NAME);
        } else if (event.getType().equals("UserTaskInstanceEvent") && userTasksEvents.orElse(true)) {

            publishToTopic(event, userTasksEventsEmitter, UI_TOPIC_NAME);
        } else if (event.getType().equals("VariableInstanceEvent") && variablesEvents.orElse(true)) {

            publishToTopic(event, variablesEventsEmitter, VI_TOPIC_NAME);
        } else {
            logger.warn("Unknown type of event '{}', ignoring", event.getType());
        }

    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        for (DataEvent<?> event : events) {
            publish(event);
        }
    }

    protected void publishToTopic(DataEvent<?> event, Emitter<String> emitter, String topic) {
        if (emitter.isCancelled()) {
            logger.debug("Emitter {} is not ready to send messages", topic);
        }

        logger.debug("About to publish event {} to topic {}", event, topic);
        try {
            String eventString = json.writeValueAsString(event);
            logger.debug("Event payload '{}'", eventString);

            emitter.send(eventString);
            logger.debug("Successfully published event {} to topic {}", event, topic);
        } catch (Exception e) {
            logger.error("Error while publishing event to topic {} for event {}", topic, event, e);
        }
    }
}
