
package io.automatik.engine.api.event;

import java.util.Collection;

/**
 * Batch of events to be considered as single item to be processed. New events
 * can be appended at any time unless the events have already been consumed - by
 * calling <code>events</code> method.
 *
 */
public interface EventBatch {

	/**
	 * Appends new event in its raw format - meaning as it was generated
	 * 
	 * @param rawEvent event to be appended to the batch
	 */
	void append(Object rawEvent);

	/**
	 * Returns all events appended to this batch already converted to
	 * <code>DataEvents</code>
	 * 
	 * @return converted events
	 */
	Collection<DataEvent<?>> events();
}
