
package io.automatiko.engine.api.event.process;

import java.util.Date;

import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;

/**
 * A runtime event related to the execution of process instances.
 */
public interface ProcessEvent {

	/**
	 * The ProcessInstance this event relates to.
	 *
	 * @return the process instance
	 */
	ProcessInstance getProcessInstance();

	/**
	 * Returns exact date when the event was created
	 * 
	 * @return time when event was created
	 */
	Date getEventDate();

	/**
	 * Returns current process runtime for this event
	 * 
	 * @return process runtime
	 */
	ProcessRuntime getProcessRuntime();

}
