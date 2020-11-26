
package io.automatik.engine.codegen.context;

import java.util.List;

import io.automatik.engine.api.config.AutomatikConfig;
import io.automatik.engine.codegen.CodeGenConstants;

public interface ApplicationBuildContext {

    AutomatikConfig config();

    boolean hasClassAvailable(String fqcn);

    List<String> classThatImplement(String fqcn);

    default boolean isValidationSupported() {
        return hasClassAvailable(CodeGenConstants.VALIDATION_CLASS);
    }

    default boolean isEntitiesSupported() {
        return hasClassAvailable(CodeGenConstants.ENTITY_CLASS);
    }
}
