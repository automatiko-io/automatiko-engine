package io.automatiko.addons.graphql;

import java.util.function.Function;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.automatiko.addons.graphql.internal.SecurityAwareBroadcastProcessor;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;

@Dependent
public class GraphQLSubscriptionEventPublisher<T> {

    SecurityAwareBroadcastProcessor<T> createdProcessor = SecurityAwareBroadcastProcessor.create();

    SecurityAwareBroadcastProcessor<T> completedProcessor = SecurityAwareBroadcastProcessor.create();

    SecurityAwareBroadcastProcessor<T> abortedProcessor = SecurityAwareBroadcastProcessor.create();

    SecurityAwareBroadcastProcessor<T> inErrorProcessor = SecurityAwareBroadcastProcessor.create();

    SecurityAwareBroadcastProcessor<T> changedProcessor = SecurityAwareBroadcastProcessor.create();

    GraphQLEventPublisher publisher;

    private String processId;

    private Function<ProcessInstance<?>, T> mapper;

    @Inject
    public GraphQLSubscriptionEventPublisher(GraphQLEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void process(DataEvent<?> event) {
        if (processId == null || mapper == null) {
            return;
        }

        if (event instanceof ProcessInstanceDataEvent) {
            ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) event;

            ProcessInstance<?> instance = piEvent.getData().sourceInstance();

            if (instance.process().id().equals(processId)) {

                if (instance.status() == ProcessInstance.STATE_COMPLETED) {
                    completedProcessor.onNext(mapper.apply(instance), piEvent.getData().getVisibleTo());
                } else if (instance.status() == ProcessInstance.STATE_ABORTED) {
                    abortedProcessor.onNext(mapper.apply(instance), piEvent.getData().getVisibleTo());
                } else if (instance.status() == ProcessInstance.STATE_ERROR) {
                    inErrorProcessor.onNext(mapper.apply(instance), piEvent.getData().getVisibleTo());
                } else {
                    changedProcessor.onNext(mapper.apply(instance), piEvent.getData().getVisibleTo());
                }
            }
        }

    }

    public void configure(String processId, Function<ProcessInstance<?>, T> mapper) {
        this.processId = processId;
        this.mapper = mapper;

        this.publisher.register(this);
    }

    public SecurityAwareBroadcastProcessor<T> created(T clazz) {
        return createdProcessor;
    }

    public SecurityAwareBroadcastProcessor<T> completed(T clazz) {
        return completedProcessor;
    }

    public SecurityAwareBroadcastProcessor<T> aborted(T clazz) {
        return abortedProcessor;
    }

    public SecurityAwareBroadcastProcessor<T> inError(T clazz) {
        return inErrorProcessor;
    }

    public SecurityAwareBroadcastProcessor<T> changed(T clazz) {
        return changedProcessor;
    }
}
