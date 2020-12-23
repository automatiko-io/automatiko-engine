
package io.automatiko.engine.api.workflow;

import java.util.List;

import io.automatiko.engine.api.event.process.ProcessEventListener;

public interface ProcessEventListenerConfig {

	List<ProcessEventListener> listeners();

}
