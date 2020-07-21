
package io.automatik.engine.workflow.base.core.validation;

import io.automatik.engine.api.definition.process.Process;

/**
 * Represents a RuleFlow validation error.
 * 
 */
public interface ProcessValidationError {

	Process getProcess();

	String getMessage();

}
