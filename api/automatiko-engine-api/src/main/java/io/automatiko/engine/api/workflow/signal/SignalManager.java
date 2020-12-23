
package io.automatiko.engine.api.workflow.signal;

import io.automatiko.engine.api.runtime.process.EventListener;

public interface SignalManager {

	void signalEvent(String type, Object event);

	void signalEvent(String id, String type, Object event);

	void addEventListener(String type, EventListener eventListener);

	void removeEventListener(String type, EventListener eventListener);

	boolean accept(String type, Object event);

}
