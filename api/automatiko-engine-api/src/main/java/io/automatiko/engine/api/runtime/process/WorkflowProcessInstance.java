
package io.automatiko.engine.api.runtime.process;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import io.automatiko.engine.api.workflow.ExecutionsErrorInfo;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.api.workflow.flexible.AdHocFragment;
import io.automatiko.engine.api.workflow.flexible.Milestone;

/**
 * A workflow process instance represents one specific instance of a workflow
 * process that is currently executing. It is an extension of a
 * <code>ProcessInstance</code> and contains all runtime state related to the
 * execution of workflow processes.
 *
 * @see io.automatiko.engine.api.runtime.process.ProcessInstance
 */
public interface WorkflowProcessInstance extends ProcessInstance, NodeInstanceContainer {

    /**
     * Returns the value of the variable with the given name. Note that only
     * variables in the process-level scope will be searched. Returns
     * <code>null</code> if the value of the variable is null or if the variable
     * cannot be found.
     *
     * @param name the name of the variable
     * @return the value of the variable, or <code>null</code> if it cannot be found
     */
    Object getVariable(String name);

    /**
     * Sets process variable with given value under given name
     * 
     * @param name name of the variable
     * @param value value of the variable
     */
    void setVariable(String name, Object value);

    /**
     * Returns start date of this process instance
     * 
     * @return actual start date
     */
    Date getStartDate();

    /**
     * Returns end date (either completed or aborted) of this process instance
     * 
     * @return actual end date
     */
    Date getEndDate();

    /**
     * Returns list of errors for this process instance.
     * 
     * @return list of errors if the process instance is in error state
     */
    List<ExecutionsErrorInfo> errors();

    /**
     * Returns optional correlation key assigned to process instance
     * 
     * @return correlation key if present otherwise null
     */
    String getCorrelationKey();

    /**
     * Returns the list of Milestones and their status in the current process
     * instances
     * 
     * @return Milestones defined in the process
     */
    Collection<Milestone> milestones();

    /**
     * @return AdHocFragments from the process instances
     */
    Collection<AdHocFragment> adHocFragments();

    /**
     * Returns current collection of tags.
     * 
     * @return collection of tags
     */
    Collection<Tag> getTags();

    /**
     * Evaluates all tags to apply any changes of the variables
     * 
     * @return collection of tags
     */
    Collection<Tag> evaluateTags();

    /**
     * Adds new tag
     * 
     * @param tag new tag to be added
     */
    void addTag(String value);

    /**
     * Removes the tag associated with given id
     * 
     * @param id identifier of the tag
     */
    boolean removedTag(String id);

    Object getMetaData(String name);

}
