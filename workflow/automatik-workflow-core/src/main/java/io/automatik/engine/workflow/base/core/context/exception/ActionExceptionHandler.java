
package io.automatik.engine.workflow.base.core.context.exception;

import java.io.Serializable;

import io.automatik.engine.workflow.process.core.ProcessAction;

public class ActionExceptionHandler implements ExceptionHandler, Serializable {

	private static final long serialVersionUID = 510l;

	private String faultVariable;
	private ProcessAction action;

	public String getFaultVariable() {
		return faultVariable;
	}

	public void setFaultVariable(String faultVariable) {
		this.faultVariable = faultVariable;
	}

	public ProcessAction getAction() {
		return action;
	}

	public void setAction(ProcessAction action) {
		this.action = action;
	}

	public String toString() {
		return action == null ? "" : action.toString();
	}

}
