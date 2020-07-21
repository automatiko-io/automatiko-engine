
package io.automatik.engine.api.workflow;

public class ProcessInstanceDuplicatedException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private final String processInstanceId;

	public ProcessInstanceDuplicatedException(String processInstanceId) {
		super("Process instance with id '" + processInstanceId
				+ "' already exists, usually this means business key has been already used");
		this.processInstanceId = processInstanceId;
	}

	public ProcessInstanceDuplicatedException(String processInstanceId, Throwable cause) {
		super("Process instance with '" + processInstanceId
				+ "' already exists, usually this means business key has been already used");
		this.processInstanceId = processInstanceId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

}
