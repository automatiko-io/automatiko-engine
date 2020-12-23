package io.automatiko.engine.decision.dmn.config;

import org.kie.dmn.api.core.event.DMNRuntimeEventListener;

public class DefaultDecisionEventListenerConfig extends CachedDecisionEventListenerConfig {

	public DefaultDecisionEventListenerConfig(DMNRuntimeEventListener... listeners) {
		for (DMNRuntimeEventListener listener : listeners) {
			register(listener);
		}
	}

}
