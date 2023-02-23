package io.automatiko.addons.graphql.ut;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.automatiko.addons.graphql.GraphQLEventPublisher;
import io.automatiko.addons.graphql.GraphQLSubscriptionEventPublisher;
import io.automatiko.addons.graphql.internal.SecurityAwareBroadcastProcessor;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;

@ApplicationScoped
public class GraphQLUserTaskSubscriptionEventPublisher implements GraphQLSubscriptionEventPublisher<UserTaskEventInput> {

    SecurityAwareBroadcastProcessor<UserTaskEventInput> userTasksProcessor = SecurityAwareBroadcastProcessor.create();

    GraphQLEventPublisher publisher;

    @Inject
    public GraphQLUserTaskSubscriptionEventPublisher(GraphQLEventPublisher publisher) {
        this.publisher = publisher;

        this.publisher.register(this);
    }

    @Override
    public void process(DataEvent<?> event) {
        if (event instanceof UserTaskInstanceDataEvent) {
            UserTaskInstanceDataEvent utEvent = (UserTaskInstanceDataEvent) event;

            userTasksProcessor.onNext(new UserTaskEventInput(utEvent.getData()), utEvent.getData().sourceInstance());
        }

    }

    public SecurityAwareBroadcastProcessor<UserTaskEventInput> userTask() {
        return userTasksProcessor;
    }
}
