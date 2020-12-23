
package io.automatiko.engine.api.jobs;

import java.time.ZonedDateTime;

/**
 * JobsService provides an entry point for working with different types of jobs
 * that are meant by default to run in background.
 *
 */
public interface JobsService {

	/**
	 * Schedules process job that is responsible for starting new process instances
	 * based on the given description.
	 * 
	 * @param description defines what kind of process should be started upon
	 *                    expiration time
	 * @return returns unique id of the job
	 */
	String scheduleProcessJob(ProcessJobDescription description);

	/**
	 * Schedules process instance related job that will signal exact same process
	 * instance upon expiration time.
	 * 
	 * @param description defines the context of the process instance that should be
	 *                    signaled
	 * @return returns unique id of the job
	 */
	String scheduleProcessInstanceJob(ProcessInstanceJobDescription description);

	/**
	 * Cancels given job
	 * 
	 * @param id unique id of the job
	 * @return returns true if the cancellation was successful, otherwise false
	 */
	boolean cancelJob(String id);

	/**
	 * Returns actual schedule time for the next expiration of given job
	 * 
	 * @param id unique id of the job
	 * @return returns actual expiration time for the job
	 */
	ZonedDateTime getScheduledTime(String id);
}
