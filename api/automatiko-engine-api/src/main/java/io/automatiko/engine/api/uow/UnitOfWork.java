
package io.automatiko.engine.api.uow;

import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstances;

/**
 * Unit of Work allows to group related activities and operation into single
 * unit. It it can be then completed or aborted as one making the execution
 * consistent.
 * 
 * Depending on the implementation it can rely on some additional frameworks or
 * capabilities to carry on with the execution semantics.
 *
 */
public interface UnitOfWork {

    /**
     * Returns unique identifier of this unit of work
     * 
     * @return identifier of the unit of work
     */
    String identifier();

    /**
     * Initiates this unit of work if not already started. It is safe to call start
     * multiple times unless the unit has already been completed or aborted.
     */
    void start();

    /**
     * Completes this unit of work ensuring all awaiting work is invoked.
     */
    void end();

    /**
     * Aborts this unit of work and ignores any awaiting work.
     */
    void abort();

    /**
     * Intercepts work that should be done as part of this unit of work.
     * 
     * @param work actual work to be invoked as part of this unit of work.
     */
    void intercept(WorkUnit<?> work);

    /**
     * Allows to manage given implementation of process instances to provide
     * up to date information for executed instances within unit of work
     * 
     * @param process definition of the process
     * @param instances concrete implementation of the <code>ProcessInstances</code>
     * @return returns managed instance of the given ProcessInstances instance
     */
    default ProcessInstances<?> managedProcessInstances(Process<?> process, ProcessInstances<?> instances) {
        return instances;
    }

}
