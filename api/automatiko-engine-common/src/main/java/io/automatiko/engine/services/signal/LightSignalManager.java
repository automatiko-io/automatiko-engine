
package io.automatiko.engine.services.signal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.api.workflow.signal.SignalManagerHub;

public class LightSignalManager implements SignalManager {

    private EventListenerResolver instanceResolver;
    private SignalManagerHub signalManagerHub;
    private ConcurrentHashMap<String, Set<EventListener>> listeners = new ConcurrentHashMap<>();

    public LightSignalManager(EventListenerResolver instanceResolver, SignalManagerHub signalManagerHub) {
        this.instanceResolver = instanceResolver;
        this.signalManagerHub = signalManagerHub;
    }

    public void addEventListener(String type, EventListener eventListener) {
        listeners.compute(type, (k, v) -> {
            if (v == null) {
                v = new CopyOnWriteArraySet<>();
            }
            v.add(eventListener);
            return v;
        });
        signalManagerHub.subscribe(type, this);
    }

    public void removeEventListener(String type, EventListener eventListener) {
        listeners.computeIfPresent(type, (k, v) -> {
            v.remove(eventListener);
            if (v.isEmpty()) {
                listeners.remove(type);
            }
            return v;
        });
        signalManagerHub.unsubscribe(type, this);
    }

    public void signalEvent(String type, Object event) {
        if (!listeners.containsKey(type)) {
            if (event instanceof ProcessInstance && listeners
                    .containsKey(((ProcessInstance) event).getProcessId() + version((ProcessInstance) event))) {
                listeners
                        .getOrDefault(((ProcessInstance) event).getProcessId() + version((ProcessInstance) event),
                                Collections.emptySet())
                        .forEach(e -> e.signalEvent(type, event));
                return;
            }
            signalManagerHub.publish(type, event);
        }
        listeners.getOrDefault(type, Collections.emptySet()).forEach(e -> e.signalEvent(type, event));
    }

    public void signalEvent(String processInstanceId, String type, Object event) {
        instanceResolver.find(processInstanceId).ifPresent(signalable -> signalable.signalEvent(type, event));
    }

    @Override
    public boolean accept(String type, Object event) {
        if (listeners.containsKey(type)) {
            return true;
        }
        // handle processInstance events that are registered as child processes
        return event instanceof ProcessInstance
                && listeners.containsKey(((ProcessInstance) event).getProcessId() + version((ProcessInstance) event));
    }

    protected Map<String, Set<EventListener>> getListeners() {
        return listeners;
    }

    protected String version(ProcessInstance pi) {
        String version = pi.getProcess().getVersion();
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }

    public void setInstanceResolver(EventListenerResolver instanceResolver) {
        this.instanceResolver = instanceResolver;
    }
}
