
package io.automatiko.engine.api.event.process;

import io.automatiko.engine.api.runtime.process.NodeInstance;

/**
 * An event when a work item has transition between life cycle phases
 */
public interface ProcessNodeInstanceFailedEvent extends ProcessEvent {

    /**
     * Returns node instance that failed at execution
     * 
     * @return work item
     */
    NodeInstance getNodeInstance();

    /**
     * Unique identifier of the error
     * 
     * @return error identifier
     */
    String getErrorId();

    /**
     * Short descriptive message of the error
     * 
     * @return error message
     */
    String getErrorMessage();

    /**
     * Returns the actual exception that was thrown at execution
     * 
     * @return transition
     */
    Exception getException();

}
