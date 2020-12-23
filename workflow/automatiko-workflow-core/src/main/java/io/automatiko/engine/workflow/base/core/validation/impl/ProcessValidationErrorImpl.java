
package io.automatiko.engine.workflow.base.core.validation.impl;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.core.validation.ProcessValidationError;

public class ProcessValidationErrorImpl implements ProcessValidationError {

	private Process process;
	private String message;

	public ProcessValidationErrorImpl(Process process, String message) {
		this.process = process;
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public Process getProcess() {
		return process;
	}

	public String toString() {
		return "Process '" + process.getName() + "' [" + process.getId() + "]: " + getMessage();
	}

}
