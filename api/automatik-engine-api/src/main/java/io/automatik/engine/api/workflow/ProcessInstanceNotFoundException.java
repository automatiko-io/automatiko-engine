
package io.automatik.engine.api.workflow;

public class ProcessInstanceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private String processInstanceId;

	public ProcessInstanceNotFoundException(String processInstanceId) {
		super("Process instance with id " + processInstanceId + " not found");
		this.processInstanceId = processInstanceId;
	}

	public ProcessInstanceNotFoundException(String processInstanceId, Throwable cause) {
		super("Process instance with id " + processInstanceId + " not found", cause);
		this.processInstanceId = processInstanceId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

}
