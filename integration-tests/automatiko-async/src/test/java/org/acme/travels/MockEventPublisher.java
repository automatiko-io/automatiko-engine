package org.acme.travels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class MockEventPublisher implements EventPublisher {

    private static List<DataEvent<?>> events = new ArrayList<>();

    @Override
    public void publish(DataEvent<?> event) {
        if (event instanceof ProcessInstanceDataEvent) {
            events.add(event);
        }
    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        for (DataEvent<?> event : events) {
            publish(event);
        }

    }

    public void clear() {
        events.clear();
    }

    public List<DataEvent<?>> events() {
        return events;
    }
}
