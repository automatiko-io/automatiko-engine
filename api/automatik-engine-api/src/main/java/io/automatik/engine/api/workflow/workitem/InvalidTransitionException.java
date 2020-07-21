
package io.automatik.engine.api.workflow.workitem;

/**
 * Thrown when given work item transition cannot be performed
 *
 */
public class InvalidTransitionException extends RuntimeException {

	private static final long serialVersionUID = -40827773509603874L;

	public InvalidTransitionException(String message) {
		super(message);
	}

}
