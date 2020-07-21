
package io.automatik.engine.api.workflow.signal;

public interface SignalManagerHub {

	void publish(String type, Object signalData);

	void publishTargeting(String id, String type, Object signalData);

	void subscribe(String type, SignalManager signalManager);

	void unsubscribe(String type, SignalManager signalManager);
}
