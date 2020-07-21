
package io.automatik.engine.api.event;

import java.util.Collection;

/**
 * Responsible for publishing events for consumption to the "outside world"
 * 
 * Depending on the implementation it can be to push over the wire or use an in
 * memory queue to notify other parties about particular events.
 * 
 * In case any filtering needs to take place, this is where it should happen.
 *
 */
public interface EventPublisher {

	/**
	 * Publishes individual event
	 * 
	 * @param event event to be published
	 */
	void publish(DataEvent<?> event);

	/**
	 * Publish collection of events. It's up to implementation to publish them
	 * individually or as complete collection.
	 * 
	 * @param events events to be published
	 */
	void publish(Collection<DataEvent<?>> events);
}
