
package io.automatik.engine.workflow;

import io.automatik.engine.api.event.process.ProcessEventListener;

public class DefaultProcessEventListenerConfig extends CachedProcessEventListenerConfig {

	public DefaultProcessEventListenerConfig(ProcessEventListener... listeners) {
		for (ProcessEventListener listener : listeners) {
			register(listener);
		}
	}
}
