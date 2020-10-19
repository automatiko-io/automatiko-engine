
package io.automatik.engine.api;

import io.automatik.engine.api.decision.DecisionModels;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.Processes;
import io.automatik.engine.api.workflow.cases.CaseDefinitions;

/**
 * Entry point for accessing business automation components such as processes,
 * rules, decisions, etc.
 * <p>
 * It should be considered as singleton kind of object that can be safely used
 * across entire application.
 */
public interface Application {

    /**
     * Returns configuration of the application
     * 
     * @return current configuration
     */
    Config config();

    /**
     * Returns processes found in the application otherwise null
     * 
     * @return processes information or null of non found
     */
    default Processes processes() {
        return null;
    }

    /**
     * Returns decision models found in the application otherwise null
     * 
     * @return decision models or null if not found
     */
    default DecisionModels decisionModels() {
        return null;
    }

    /**
     * Returns cases (case definitions) found in the application otherwise null
     * 
     * @return cases (case definitions) or null if not found
     */
    default CaseDefinitions cases() {
        return null;
    }

    /**
     * Returns unit of work manager that allows to control execution within the
     * application
     * 
     * @return non null unit of work manager
     */
    UnitOfWorkManager unitOfWorkManager();
}
