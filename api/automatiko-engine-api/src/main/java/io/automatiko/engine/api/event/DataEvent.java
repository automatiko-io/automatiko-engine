
package io.automatiko.engine.api.event;

/**
 * Represents top level data event structure that can be emitted from within
 * running process, decision or rule.
 * 
 * It's main aim is to be transferred over the wire but the event itself is not
 * meant to do transformation to be "wire-friendly"
 * 
 * Main point of the event is to be compatible with cloud events specification
 * and thus comes with main fields that the spec defines.
 * 
 * Classes implementing can provide more information to be considered extensions
 * of the event - see cloud event extension elements.
 *
 * @param <T> type of the body of the event
 */
public interface DataEvent<T> {

	/**
	 * Returns specification version of the cloud event
	 * 
	 * @return specification version
	 */
	String getSpecversion();

	/**
	 * Returns unique id of the event
	 * 
	 * @return unique event id
	 */
	String getId();

	/**
	 * Returns type of the event this instance represents e.g. ProcessInstanceEvent
	 * 
	 * @return type of the event
	 */
	String getType();

	/**
	 * Returns source of the event that is in URI syntax
	 * 
	 * @return uri source
	 */
	String getSource();

	/**
	 * Returns returns time when the event was created
	 * 
	 * @return time of the event
	 */
	String getTime();

	/**
	 * Returns actual body of the event
	 * 
	 * @return
	 */
	T getData();
}
