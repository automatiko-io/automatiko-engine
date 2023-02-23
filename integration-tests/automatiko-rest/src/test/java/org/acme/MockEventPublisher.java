package org.acme;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.impl.NodeInstanceEventBody;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class MockEventPublisher implements EventPublisher {

    private static List<DataEvent<?>> events = new CopyOnWriteArrayList<>();

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

    public boolean waitForNodeEntered(String nodeName, long elapsed) throws InterruptedException {

        boolean found = false;

        while (elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);

            for (DataEvent<?> event : events) {

                ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) event;

                found = piEvent.getData().getNodeInstances().stream()
                        .anyMatch(ni -> ni.getNodeName().equals(nodeName) && ni.getLeaveTime() == null);

                if (found) {
                    break;
                }
            }

            if (found) {
                break;
            }
        }

        return found;
    }

    public boolean waitForNodeCompleted(String nodeName, long elapsed) throws InterruptedException {

        boolean found = false;

        while (elapsed >= 0) {
            elapsed -= 1000;
            Thread.sleep(1000);

            for (DataEvent<?> event : events) {

                ProcessInstanceDataEvent piEvent = (ProcessInstanceDataEvent) event;

                found = piEvent.getData().getNodeInstances().stream().map(ni -> log(ni))
                        .anyMatch(ni -> ni.getNodeName().equals(nodeName) && ni.getLeaveTime() != null);

                if (found) {
                    break;
                }
            }

            if (found) {
                break;
            }
        }

        return found;
    }

    private NodeInstanceEventBody log(NodeInstanceEventBody ni) {

        return ni;
    }
}
