
package io.automatik.engine.api.workflow.workitem;

/**
 * Thrown when there is no such life cycle phase
 *
 */
public class InvalidLifeCyclePhaseException extends RuntimeException {

	private static final long serialVersionUID = -40827773509603874L;

	public InvalidLifeCyclePhaseException(String message) {
		super(message);
	}

}
