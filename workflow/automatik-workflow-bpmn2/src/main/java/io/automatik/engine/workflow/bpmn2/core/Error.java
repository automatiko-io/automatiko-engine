
package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Error extends Signal implements Serializable {

	private static final long serialVersionUID = 510l;

	private String errorCode;

	public Error(String id, String errorCode, String itemRef) {
		super(id, itemRef);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

}
