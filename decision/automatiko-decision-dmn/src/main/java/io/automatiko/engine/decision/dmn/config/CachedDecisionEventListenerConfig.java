package io.automatiko.engine.decision.dmn.config;

import java.util.ArrayList;
import java.util.List;

import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

import io.automatiko.engine.api.decision.DecisionEventListenerConfig;

public class CachedDecisionEventListenerConfig implements DecisionEventListenerConfig<DMNRuntimeEventListener> {
	private final List<DMNRuntimeEventListener> listeners;

	public CachedDecisionEventListenerConfig() {
		listeners = new ArrayList<>();
	}

	public CachedDecisionEventListenerConfig(List<DMNRuntimeEventListener> listeners) {
		this.listeners = listeners;
	}

	public CachedDecisionEventListenerConfig register(DMNRuntimeEventListener listener) {
		listeners.add(listener);
		return this;
	}

	@Override
	public List<DMNRuntimeEventListener> listeners() {
		return listeners;
	}
}
