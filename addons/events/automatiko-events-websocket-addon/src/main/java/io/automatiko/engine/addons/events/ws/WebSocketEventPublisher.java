package io.automatiko.engine.addons.events.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class WebSocketEventPublisher implements EventPublisher {

    public static final String INSTANCE_KEY = "quarkus.automatiko.events.websocket.instance";
    public static final String TASKS_KEY = "quarkus.automatiko.events.websocket.tasks";

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    private ObjectMapper json;

    private Optional<Boolean> instance;

    private Optional<Boolean> tasks;

    @Inject
    public WebSocketEventPublisher(ObjectMapper json,
            @ConfigProperty(name = INSTANCE_KEY) Optional<Boolean> instance,
            @ConfigProperty(name = TASKS_KEY) Optional<Boolean> tasks) {
        this.json = json;
        this.instance = instance;
        this.tasks = tasks;

    }

    @Override
    public void publish(DataEvent<?> event) {

        if (event instanceof ProcessInstanceDataEvent && !instance.orElse(true)) {
            LOGGER.debug("Skipping process instance event as the publisher should not deal with instances");
            return;
        } else if (event instanceof UserTaskInstanceDataEvent && !tasks.orElse(true)) {
            LOGGER.debug("Skipping user task event as the publisher should not deal with tasks");
            return;
        }

        String text;
        try {
            text = json.writeValueAsString(event);

            for (Session session : sessions.values()) {

                String filter = (String) session.getUserProperties().get("atk_filter");
                if (filter != null && !filter.matches(event.getType())) {
                    continue;
                }

                boolean allowed = true;
                IdentityProvider identityProvider = (IdentityProvider) session.getUserProperties().get("atk_identity");
                if (event instanceof ProcessInstanceDataEvent) {

                    List<String> visibleTo = ((ProcessInstanceDataEvent) event).getData().getVisibleTo();
                    allowed = visibleTo.isEmpty() || visibleTo.contains(identityProvider.getName())
                            || visibleTo.stream().anyMatch(item -> identityProvider.getRoles().contains(item));

                } else if (event instanceof UserTaskInstanceDataEvent) {

                    HumanTaskWorkItem workItem = ((UserTaskInstanceDataEvent) event).getData().sourceInstance();
                    allowed = workItem.enforce(SecurityPolicy.of(identityProvider));
                }
                if (allowed) {
                    session.getAsyncRemote().sendText(text);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error when publishing websocket event", e);
        }

    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        for (DataEvent<?> event : events) {
            publish(event);
        }
    }

    public void add(String id, Session session) {
        this.sessions.put(id, session);
    }

    public void remove(String id) {
        this.sessions.remove(id);
    }
}
