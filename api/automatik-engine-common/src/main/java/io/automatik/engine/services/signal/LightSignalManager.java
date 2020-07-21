
package io.automatik.engine.services.signal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.api.workflow.signal.SignalManagerHub;

public class LightSignalManager implements SignalManager {

	private SignalManagerHub signalManagerHub;
	private final EventListenerResolver instanceResolver;
	private ConcurrentHashMap<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

	public LightSignalManager(EventListenerResolver instanceResolver, SignalManagerHub signalManagerHub) {
		this.instanceResolver = instanceResolver;
		this.signalManagerHub = signalManagerHub;
	}

	public void addEventListener(String type, EventListener eventListener) {
		listeners.compute(type, (k, v) -> {
			if (v == null) {
				v = new CopyOnWriteArrayList<>();
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
			if (event instanceof ProcessInstance && listeners.containsKey(((ProcessInstance) event).getProcessId())) {
				listeners.getOrDefault(((ProcessInstance) event).getProcessId(), Collections.emptyList())
						.forEach(e -> e.signalEvent(type, event));
				return;
			}
			signalManagerHub.publish(type, event);
		}
		listeners.getOrDefault(type, Collections.emptyList()).forEach(e -> e.signalEvent(type, event));
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
		return event instanceof ProcessInstance && listeners.containsKey(((ProcessInstance) event).getProcessId());
	}
}
