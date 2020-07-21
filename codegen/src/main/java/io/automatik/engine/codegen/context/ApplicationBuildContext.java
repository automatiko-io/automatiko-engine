
package io.automatik.engine.codegen.context;

import io.automatik.engine.codegen.CodeGenConstants;

public interface ApplicationBuildContext {

	boolean hasClassAvailable(String fqcn);

	default boolean isValidationSupported() {
		return hasClassAvailable(CodeGenConstants.VALIDATION_CLASS);
	}
}
