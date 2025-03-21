
package io.automatiko.engine.codegen.context;

import java.util.List;

import io.automatiko.engine.codegen.CodeGenConstants;
import io.automatiko.engine.quarkus.AutomatikoBuildTimeConfig;

public interface ApplicationBuildContext {

    AutomatikoBuildTimeConfig config();

    boolean hasClassAvailable(String fqcn);

    List<String> classThatImplement(String fqcn);

    boolean hasCapability(String capability);

    default boolean isValidationSupported() {
        return hasClassAvailable(CodeGenConstants.VALIDATION_CLASS);
    }

    default boolean isEntitiesSupported() {
        return hasClassAvailable(CodeGenConstants.ENTITY_CLASS);
    }

    default boolean isOpenApiSupported() {
        return hasClassAvailable(CodeGenConstants.OPENA_API_SCHEMA_CLASS);
    }

    default boolean isUserTaskMgmtSupported() {
        return hasClassAvailable(CodeGenConstants.USERTASK_MGMT_DATA_CLASS);
    }

    default boolean isGraphQLSupported() {
        return hasClassAvailable(CodeGenConstants.GRAPHQL_CLASS);
    }

    default boolean isDmnSupported() {
        return hasClassAvailable(CodeGenConstants.DMN_CLASS);
    }

    default boolean isTracingSupported() {
        return hasCapability("io.quarkus.opentelemetry.tracer");
    }
}
