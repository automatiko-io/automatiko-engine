
package io.automatiko.engine.workflow;

import java.util.ArrayList;
import java.util.List;

import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.workflow.ProcessEventListenerConfig;

public class CachedProcessEventListenerConfig implements ProcessEventListenerConfig {

	private final List<ProcessEventListener> processEventListeners;

	public CachedProcessEventListenerConfig() {
		this.processEventListeners = new ArrayList<>();
	}

	public CachedProcessEventListenerConfig(List<ProcessEventListener> processEventListeners) {
		this.processEventListeners = processEventListeners;
	}

	public CachedProcessEventListenerConfig register(ProcessEventListener listener) {
		processEventListeners.add(listener);
		return this;
	}

	@Override
	public List<ProcessEventListener> listeners() {
		return processEventListeners;
	}

}
