package io.automatiko.engine.addons.events.ws;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;

@ApplicationScoped
public class WebSocketEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    private ObjectMapper json;

    @Inject
    public WebSocketEventPublisher(ObjectMapper json) {
        this.json = json;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void publish(DataEvent<?> event) {

        String text;
        try {
            text = json.writeValueAsString(event);

            for (Session session : sessions.values()) {
                boolean allowed = true;
                IdentityProvider identityProvider = (IdentityProvider) session.getUserProperties().get("atk_identity");
                if (event instanceof ProcessInstanceDataEvent) {

                    ProcessInstance pinstance = ((ProcessInstanceDataEvent) event).getData().sourceInstance();
                    allowed = pinstance.process().accessPolicy().canReadInstance(identityProvider, pinstance);

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
