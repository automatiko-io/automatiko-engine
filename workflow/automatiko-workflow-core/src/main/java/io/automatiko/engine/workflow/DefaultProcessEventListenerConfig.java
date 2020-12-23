
package io.automatiko.engine.workflow;

import io.automatiko.engine.api.event.process.ProcessEventListener;

public class DefaultProcessEventListenerConfig extends CachedProcessEventListenerConfig {

	public DefaultProcessEventListenerConfig(ProcessEventListener... listeners) {
		for (ProcessEventListener listener : listeners) {
			register(listener);
		}
	}
}
