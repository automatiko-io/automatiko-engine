package io.automatiko.addons.graphql;

import io.automatiko.engine.api.event.DataEvent;

public interface GraphQLSubscriptionEventPublisher<T> {

    void process(DataEvent<?> event);
}
