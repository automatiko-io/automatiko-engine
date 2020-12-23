
package io.automatiko.engine.api.workflow;

/**
 * Thrown when there is problem encountered during process instance execution.
 * Usually caused by one of the node instances not able to perform desired
 * action.
 * 
 */
public class ProcessInstanceExecutionException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private String processInstanceId;
	private String failedNodeId;
	private String errorMessage;

	public ProcessInstanceExecutionException(String processInstanceId, String failedNodeId, String errorMessage) {
		super("Process instance with id " + processInstanceId + " failed to execute due to " + errorMessage);
		this.processInstanceId = processInstanceId;
		this.failedNodeId = failedNodeId;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns process instance id of the instance that failed.
	 * 
	 * @return process instance id
	 */
	public String getProcessInstanceId() {
		return processInstanceId;
	}

	/**
	 * Returns node definition id of the node instance that failed to execute
	 * 
	 * @return node definition id
	 */
	public String getFailedNodeId() {
		return failedNodeId;
	}

	/**
	 * Returns error message associated with this failure. Usually will consists of
	 * error id, fully qualified class name of the root cause exception and error
	 * message
	 * 
	 * @return error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

}
