package io.automatiko.engine.addons.events.ws;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.IdentitySupplier;

@ServerEndpoint("/process/events")
@ApplicationScoped
public class ProcessEventsEndpoint {

    WebSocketEventPublisher publisher;

    IdentitySupplier identitySupplier;

    @Inject
    public ProcessEventsEndpoint(WebSocketEventPublisher publisher, IdentitySupplier identitySupplier) {
        this.publisher = publisher;
        this.identitySupplier = identitySupplier;
    }

    @OnOpen
    public void onOpen(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        IdentityProvider identityProvider = identitySupplier.buildIdentityProvider(
                params.getOrDefault("user", Collections.singletonList(null)).get(0), params.get("groups"));
        session.getUserProperties().put("atk_identity", identityProvider);
        session.getUserProperties().put("atk_filter", params.getOrDefault("filter", Collections.singletonList(null)).get(0));
        publisher.add(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        publisher.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        publisher.remove(session.getId());
    }

}
