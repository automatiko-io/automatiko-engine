
package io.automatik.engine.api.workflow;

import java.util.List;

import io.automatik.engine.api.event.process.ProcessEventListener;

public interface ProcessEventListenerConfig {

	List<ProcessEventListener> listeners();

}
