
package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.flexible.AdHocFragment;
import io.automatiko.engine.api.workflow.flexible.Milestone;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;

public interface ProcessInstance<T> {

    int STATE_RECOVERING = -1;
    int STATE_PENDING = 0;
    int STATE_ACTIVE = 1;
    int STATE_COMPLETED = 2;
    int STATE_ABORTED = 3;
    int STATE_SUSPENDED = 4;
    int STATE_ERROR = 5;

    /**
     * Returns process definition associated with this process instance
     *
     * @return process definition of this process instance
     */
    Process<T> process();

    /**
     * Starts process instance
     */
    void start();

    /**
     * Starts process instance with trigger
     *
     * @param trigger name of the trigger that will indicate what start node to
     *        trigger
     * @param referenceId optional reference id that points to a another component
     *        triggering this instance
     */
    void start(String trigger, String referenceId, Object data);

    /**
     * Starts process instance from given node
     *
     * @param nodeId node id that should be used as the first node
     */
    void startFrom(String nodeId);

    /**
     * Starts process instance from given node
     *
     * @param nodeId node id that should be used as the first node
     * @param referenceId optional reference id that points to a another component
     *        triggering this instance
     */
    void startFrom(String nodeId, String referenceId);

    /**
     * Sends given signal into this process instance
     *
     * @param signal signal to be processed
     */
    <S> void send(Signal<S> signal);

    /**
     * Aborts this process instance
     */
    void abort();

    /**
     * Returns process variables of this process instance
     *
     * @return variables of the process instance
     */
    T variables();

    /**
     * Updates process variables of this process instance
     */
    void updateVariables(T updates);

    /**
     * Returns current status of this process instance
     *
     * @return the current status
     */
    int status();

    /**
     * Returns collection of currently active subprocess instances where this
     * process instance is the parent;
     * 
     * @return all active subprocesses if any or empty collection
     */
    Collection<ProcessInstance<? extends Model>> subprocesses();

    /**
     * Returns collection of currently active subprocess instances where this
     * process instance is the parent;
     * 
     * @param mode mode that process instance should be loaded with
     * @return all active subprocesses if any or empty collection
     */
    Collection<ProcessInstance<? extends Model>> subprocesses(ProcessInstanceReadMode mode);

    /**
     * Completes work item belonging to this process instance with given variables
     *
     * @param id id of the work item to complete
     * @param variables optional variables
     * @param policies optional list of policies to be enforced
     * @throws WorkItemNotFoundException in case work item with given id does not
     *         exist
     */
    void completeWorkItem(String id, Map<String, Object> variables, Policy<?>... policies);

    /**
     * Aborts work item belonging to this process instance
     *
     * @param id id of the work item to complete
     * @param policies optional list of policies to be enforced
     * @throws WorkItemNotFoundException in case work item with given id does not
     *         exist
     */
    void abortWorkItem(String id, Policy<?>... policies);

    /**
     * Marks work item as failure to allow triggering of error handling routines if any
     * 
     * @param id id of the work item to complete
     * @param error actual error that happened during execution
     * @throws WorkItemNotFoundException in case work item with given id does not
     *         exist
     */
    void failWorkItem(String id, Throwable error);

    /**
     * Transition work item belonging to this process instance not another life
     * cycle phase
     *
     * @param id id of the work item to complete
     * @param transition target transition including phase, identity and data
     * @throws WorkItemNotFoundException in case work item with given id does not
     *         exist
     */
    void transitionWorkItem(String id, Transition<?> transition);

    /**
     * Returns work item identified by given id if found
     *
     * @param workItemId id of the work item
     * @param policies optional list of policies to be enforced
     * @return work item with its parameters if found
     * @throws WorkItemNotFoundException in case work item with given id does not
     *         exist
     */
    WorkItem workItem(String workItemId, Policy<?>... policies);

    /**
     * Returns list of currently active work items.
     *
     * @param policies optional list of policies to be enforced
     * @return non empty list of identifiers of currently active tasks.
     */
    List<WorkItem> workItems(Policy<?>... policies);

    /**
     * Returns identifier of this process instance
     *
     * @return id of the process instance
     */
    String id();

    /**
     * Returns optional business key associated with this process instance
     *
     * @return business key if available otherwise null
     */
    String businessKey();

    /**
     * Returns optional description of this process instance
     *
     * @return description of the process instance
     */
    String description();

    /**
     * Returns id of the parent process instance if this instance was started as subprocess
     * 
     * @return instance id of the parent instance if available otherwise null
     */
    String parentProcessInstanceId();

    /**
     * Returns id of the root process instance that this instance belongs to, if null it is then root instance itself
     * 
     * @return instance id of the root instance if available otherwise null
     */
    String rootProcessInstanceId();

    /**
     * Returns id of the root process definition that this instance belongs to, if null it is then root instance itself
     * 
     * @return definition id of the root process if available otherwise null
     */
    String rootProcessId();

    /**
     * Returns startDate of this process instance
     * 
     * @return
     */
    Date startDate();

    /**
     * Returns process errors in case process instance is in error state.
     *
     * @return returns process errors
     */
    Optional<ProcessErrors> errors();

    /**
     * Returns optional initiator of the process instance
     * 
     * @return initiator (user id) if present
     */
    Optional<String> initiator();

    /**
     * Returns tags associated with this process instances
     * 
     * @return currently associated tags
     */
    Tags tags();

    /**
     * Returns instance metadata information
     * 
     * @return metadata information about this instance
     */
    default InstanceMetadata metadata() {
        return InstanceMetadata.of(this);
    }

    /**
     * Triggers a node with given id to perform its associated logic
     * 
     * @param nodeId unique id of the node to trigger
     */
    void triggerNode(String nodeId);

    /**
     * Cancels node instance with given nodeInstanceId which results in abort of
     * any work being done by this node instance
     * 
     * @param nodeInstanceId unique id of the node instance to be cancelled
     */
    void cancelNodeInstance(String nodeInstanceId);

    /**
     * Retriggers (canceling and triggering again) node instance with given nodeInstanceId
     * This results in redoing the logic associated with given node instance
     * 
     * @param nodeInstanceId unique id of the node instance to be cancelled
     */
    void retriggerNodeInstance(String nodeInstanceId);

    /**
     * Returns all active events that this process instance is capable of acting on.
     * 
     * @return set of event descriptions for this process instance
     */
    Set<EventDescription<?>> events();

    /**
     * Returns the process milestones
     *
     * @return All the process milestones with their current status
     */
    Collection<Milestone> milestones();

    /**
     * Returns the process adHocFragments
     *
     * @return All the {@link AdHocFragment} in the process
     */
    Collection<AdHocFragment> adHocFragments();

    /**
     * Allows manually disconnect process instance to free up any taken resources.
     * It is only required if unit of work is not used.
     */
    void disconnect();

    /**
     * Returns process image annotated with active nodes
     * 
     * @param path current path that allows to build image links
     * 
     * @return annotated process instance image
     */
    String image(String path);

    /**
     * Archives this process instance by collecting all relevant information of it.
     * 
     * @param builder an archive build implementation
     * @return returns archived representation of this process instance
     */
    ArchivedProcessInstance archive(ArchiveBuilder builder);

}
