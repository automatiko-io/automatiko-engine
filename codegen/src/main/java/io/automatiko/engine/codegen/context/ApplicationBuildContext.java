
package io.automatiko.engine.codegen.context;

import java.util.List;

import io.automatiko.engine.api.config.AutomatikoConfig;
import io.automatiko.engine.codegen.CodeGenConstants;

public interface ApplicationBuildContext {

    AutomatikoConfig config();

    boolean hasClassAvailable(String fqcn);

    List<String> classThatImplement(String fqcn);

    default boolean isValidationSupported() {
        return hasClassAvailable(CodeGenConstants.VALIDATION_CLASS);
    }

    default boolean isEntitiesSupported() {
        return hasClassAvailable(CodeGenConstants.ENTITY_CLASS);
    }
}
