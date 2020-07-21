
package io.automatik.engine.workflow.base.core.validation;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.io.Resource;

/**
 * A validator for validating a RuleFlow process.
 * 
 */
public interface ProcessValidator {

	ProcessValidationError[] validateProcess(Process process);

	boolean accept(Process process, Resource resource);

	boolean compilationSupported();

}
