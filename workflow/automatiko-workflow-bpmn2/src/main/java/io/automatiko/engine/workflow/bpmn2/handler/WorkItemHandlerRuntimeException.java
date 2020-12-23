package io.automatiko.engine.workflow.bpmn2.handler;

import java.util.*;

import io.automatiko.engine.api.runtime.process.WorkItemHandler;

/**
 * This exception provides extra information about the WorkItemHandler operation
 * called to catchers of this exception. It is only meant to be thrown from a
 * {@link WorkItemHandler} instance method.
 */
public class WorkItemHandlerRuntimeException extends RuntimeException {

	/** Generated serial version uid */
	private static final long serialVersionUID = 1217036861831832336L;

	public final static String WORKITEMHANDLERTYPE = "workItemHandlerType";

	private HashMap<String, Object> info = new HashMap<String, Object>();

	public WorkItemHandlerRuntimeException(Throwable cause, String message) {
		super(message, cause);
	}

	public WorkItemHandlerRuntimeException(Throwable cause) {
		super(cause);
	}

	public void setInformation(String informationName, Object information) {
		this.info.put(informationName, information);
	}

	public Map<String, Object> getInformationMap() {
		return Collections.unmodifiableMap(this.info);
	}

}
