
package io.automatiko.engine.workflow.base.core.validation;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.io.Resource;

/**
 * A validator for validating a RuleFlow process.
 * 
 */
public interface ProcessValidator {

	ProcessValidationError[] validateProcess(Process process);

	boolean accept(Process process, Resource resource);

	boolean compilationSupported();

}
