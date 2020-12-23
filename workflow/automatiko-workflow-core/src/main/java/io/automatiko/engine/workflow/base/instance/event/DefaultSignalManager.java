
package io.automatiko.engine.workflow.base.instance.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;

public class DefaultSignalManager implements SignalManager {

	private Map<String, List<EventListener>> processEventListeners = new ConcurrentHashMap<String, List<EventListener>>();
	private InternalProcessRuntime runtime;

	public DefaultSignalManager(InternalProcessRuntime runtime) {
		this.runtime = runtime;
	}

	public void addEventListener(String type, EventListener eventListener) {
		List<EventListener> eventListeners = processEventListeners.get(type);
		// this first "if" is not pretty, but allows to synchronize only when needed
		if (eventListeners == null) {
			synchronized (processEventListeners) {
				eventListeners = processEventListeners.get(type);
				if (eventListeners == null) {
					eventListeners = new CopyOnWriteArrayList<EventListener>();
					processEventListeners.put(type, eventListeners);
				}
			}
		}
		eventListeners.add(eventListener);
	}

	public void removeEventListener(String type, EventListener eventListener) {
		if (processEventListeners != null) {
			List<EventListener> eventListeners = processEventListeners.get(type);
			if (eventListeners != null) {
				eventListeners.remove(eventListener);
				if (eventListeners.isEmpty()) {
					processEventListeners.remove(type);
					eventListeners = null;
				}
			}
		}
	}

	public void signalEvent(String type, Object event) {
		((DefaultSignalManager) runtime.getSignalManager()).internalSignalEvent(type, event);
	}

	public void internalSignalEvent(String type, Object event) {
		if (processEventListeners != null) {
			List<EventListener> eventListeners = processEventListeners.get(type);
			if (eventListeners != null) {
				for (EventListener eventListener : eventListeners) {
					eventListener.signalEvent(type, event);
				}
			}
		}
	}

	public void signalEvent(String processInstanceId, String type, Object event) {
		ProcessInstance processInstance = runtime.getProcessInstance(processInstanceId);
		if (processInstance != null) {
			processInstance.signalEvent(type, event);
		}
	}

	@Override
	public boolean accept(String type, Object event) {
		return processEventListeners.containsKey(type);
	}
}
