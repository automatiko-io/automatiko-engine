package io.automatiko.addons.graphql;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;

@ApplicationScoped
public class GraphQLEventPublisher implements EventPublisher {

    private Set<GraphQLSubscriptionEventPublisher<?>> subscribers = new HashSet<>();

    @Override
    public void publish(DataEvent<?> event) {

        subscribers.forEach(s -> s.process(event));

    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        events.forEach(this::publish);

    }

    public void register(GraphQLSubscriptionEventPublisher<?> subscriber) {
        this.subscribers.add(subscriber);
    }

}
