
package io.automatik.engine.codegen.context;

import io.automatik.engine.api.config.AutomatikConfig;
import io.automatik.engine.codegen.CodeGenConstants;

public interface ApplicationBuildContext {

	AutomatikConfig config();

	boolean hasClassAvailable(String fqcn);

	default boolean isValidationSupported() {
		return hasClassAvailable(CodeGenConstants.VALIDATION_CLASS);
	}
}
