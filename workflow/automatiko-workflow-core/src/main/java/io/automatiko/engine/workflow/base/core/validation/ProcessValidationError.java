
package io.automatiko.engine.workflow.base.core.validation;

import io.automatiko.engine.api.definition.process.Process;

/**
 * Represents a RuleFlow validation error.
 * 
 */
public interface ProcessValidationError {

	Process getProcess();

	String getMessage();

}
