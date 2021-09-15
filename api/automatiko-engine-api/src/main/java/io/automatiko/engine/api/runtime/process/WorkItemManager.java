
package io.automatiko.engine.api.runtime.process;

import java.util.Map;

import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;

/**
 * A work item manager is responsible for finding the right work item handler
 * when a work item should be executed and should be notified when this work
 * item has been completed (or aborted).
 */
public interface WorkItemManager {

    /**
     * Notifies the work item manager that the work item with the given id has been
     * completed. Results related to the execution of this work item can be passed.
     *
     * @param id the id of the work item that has been completed
     * @param results the results related to this work item, or <code>null</code> if
     *        there are no results
     */
    void completeWorkItem(String id, Map<String, Object> results, Policy<?>... policies);

    /**
     * Notifies the work item manager that the work item with the given id could not
     * be executed and should be aborted.
     *
     * @param id the id of the work item that should be aborted
     */
    void abortWorkItem(String id, Policy<?>... policies);

    /**
     * Register the given handler for all work items of the given type of work
     *
     * @param workItemName the type of work this work item handler can execute
     * @param handler the handler for executing work items
     */
    void registerWorkItemHandler(String workItemName, WorkItemHandler handler);

    /**
     * Transition work item with given id into the next life cycle phase.
     * 
     * @param id work item id to be transitioned
     * @param transition actual transition to apply to work item
     */
    default void transitionWorkItem(String id, Transition<?> transition) {

    }

    void dispose();

    void retryWorkItem(String workItemID, Map<String, Object> params);

    /**
     * Notifies the work item manager that the work item with the given id could not
     * be executed and should be marked as failed.
     *
     * @param id the id of the work item that should be aborted
     * @param error error that occured during execution
     */
    void failWorkItem(String id, Throwable error);
}
