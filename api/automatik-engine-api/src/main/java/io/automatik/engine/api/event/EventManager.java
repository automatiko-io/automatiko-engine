
package io.automatik.engine.api.event;

import io.automatik.engine.api.Addons;

/**
 * Event manager that is entry point for handling events generated during
 * execution.
 *
 */
public interface EventManager {

	/**
	 * Returns new batch instance, that should be used just by one processing thread
	 * 
	 * @return new batch instance
	 */
	EventBatch newBatch();

	/**
	 * Publishes events of the batch with main restriction that the batch is
	 * processed only when there are any publishers available.
	 * 
	 * @param batch batch to be published
	 */
	void publish(EventBatch batch);

	/**
	 * Adds given publisher to the event manager's list of publishers. Multiple
	 * publishers can be added and each will be invoked with exact same events.
	 * 
	 * @param publisher publisher to be added
	 */
	void addPublisher(EventPublisher publisher);

	/**
	 * Sets the service information that will be attached to events as source. This
	 * is expected to be URL like structure that will allow consumer of the events
	 * to navigate back.
	 * 
	 * @param service endpoint of the service
	 */
	void setService(String service);

	/**
	 * Optionally adds available addons in the running service
	 * 
	 * @param addons addons available in the service
	 */
	void setAddons(Addons addons);
}
