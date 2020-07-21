
package io.automatik.engine.api.workflow;

/**
 * Thrown when there is any kind of variable violation such as missing required
 * variable or attempt to set already defined readonly variable.
 * 
 */
public class VariableViolationException extends RuntimeException {

	private static final long serialVersionUID = 8031225233775014572L;

	private final String processInstanceId;
	private final String variableName;
	private final String errorMessage;

	public VariableViolationException(String processInstanceId, String variableName, String errorMessage) {
		super("Variable '" + variableName + "' in process instance '"
				+ (processInstanceId == null ? "unknown" : processInstanceId) + "' violated");
		this.processInstanceId = processInstanceId;
		this.variableName = variableName;
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
	 * Returns variable name that was violated
	 * 
	 * @return variable name
	 */
	public String getVariableName() {
		return variableName;
	}

	/**
	 * Returns error message associated with this failure.
	 * 
	 * @return error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

}
