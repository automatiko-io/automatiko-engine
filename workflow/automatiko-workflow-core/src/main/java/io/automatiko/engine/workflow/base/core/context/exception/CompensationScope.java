
package io.automatiko.engine.workflow.base.core.context.exception;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

/**
 * This scope represents the collection of compensation handlers in a
 * (sub)process.
 * 
 *
 */
public class CompensationScope extends ExceptionScope {

	private static final long serialVersionUID = 510l;

	public static final String COMPENSATION_SCOPE = "CompensationScope";
	public static final String IMPLICIT_COMPENSATION_PREFIX = "implicit:";

	private String containerId;

	public String getType() {
		return COMPENSATION_SCOPE;
	}

	public void setContextContainer(ContextContainer contextContainer) {
		assert contextContainer instanceof NodeContainer : "CompensationScope context container instance is NOT an instance of a node container! "
				+ "(" + contextContainer.getClass().getSimpleName() + ")";
		super.setContextContainer(contextContainer);
		if (contextContainer instanceof NodeImpl) {
			containerId = (String) ((NodeImpl) contextContainer).getMetaData("UniqueId");
		} else if (contextContainer instanceof ExecutableProcess) {
			containerId = (String) ((Process) contextContainer).getId();
		}
	}

	public String getContextContainerId() {
		return containerId;
	}

	public ExceptionHandler getExceptionHandler(String exception) {
		return exceptionHandlers.get(exception);
	}

	/**
	 * Resolves in one of two cases: when the (String) activityRefStr is equal to:
	 * 1. the id of an activity that has a compensation handler. This could be a
	 * task with a compensation boundary event or a sub-process that contains a
	 * compensation event sub-process. 2. "general:" + the id of the (sub)process
	 * that contains the compensation handler. In this case, we are signalling the
	 * "implicit compensation handler", a.k.a. broadcast/general compensation.
	 */

	public Context resolveContext(Object activityRefStr) {
		if (activityRefStr == null || !(activityRefStr instanceof String)) {
			throw new IllegalArgumentException(
					"CompensationScope can only resolve based on node id strings: " + activityRefStr);
		}
		String activityRef = (String) activityRefStr;
		if (getExceptionHandler(activityRef) != null) {
			return this;
		}
		if (activityRef.startsWith(IMPLICIT_COMPENSATION_PREFIX)) {
			String containerRef = activityRef.substring(IMPLICIT_COMPENSATION_PREFIX.length());
			if (containerId.equals(containerRef)) {
				return this;
			}
		}
		return null;
	}

}