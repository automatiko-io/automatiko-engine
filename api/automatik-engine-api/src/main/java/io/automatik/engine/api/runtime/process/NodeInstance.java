
package io.automatik.engine.api.runtime.process;

import java.util.Date;

import io.automatik.engine.api.definition.process.Node;

/**
 * A node instance represents the execution of one specific node in a process
 * instance. Whenever a node is reached during the execution of a process
 * instance, a node instance will be created. A node instance contains all the
 * runtime state related to the execution of that node. Multiple node instances
 * for the same node can coexist in the same process instance (if that node is
 * to be executed multiple times in that process instance).
 *
 * A node instance is uniquely identified (within its node instance container!)
 * by an id.
 *
 * Node instances can be nested, meaning that a node instance can be created as
 * part of another node instance.
 */
public interface NodeInstance {

    /**
     * The id of the node instance. This is unique within the node instance
     * container this node instance lives in.
     *
     * @return the id of the node instance
     */
    String getId();

    /**
     * The id of the node this node instance refers to. The node represents the
     * definition that this node instance was based on.
     *
     * @return the id of the node this node instance refers to
     */
    long getNodeId();

    /**
     * The id of the node definition this node instance refers to. The node
     * represents the definition that this node instance was based on.
     *
     * @return the definition id of the node this node instance refers to
     */
    String getNodeDefinitionId();

    /**
     * Return the node this node instance refers to. The node represents the
     * definition that this node instance was based on.
     *
     * @return the node this node instance refers to
     */
    Node getNode();

    /**
     * The name of the node this node instance refers to.
     * 
     * @return the name of the node this node instance refers to
     */
    String getNodeName();

    /**
     * The process instance that this node instance is executing in.
     * 
     * @return the process instance that this node instance is executing in
     */
    WorkflowProcessInstance getProcessInstance();

    /**
     * The node instance container that this node instance is part of. If the node
     * was defined in the top-level process scope, this is the same as the process
     * instance. If not, it is the node instance container this node instance is
     * executing in.
     *
     * @return the process instance that this node instance is executing in
     */
    NodeInstanceContainer getNodeInstanceContainer();

    /**
     * Returns variable associated with this node instance under given name
     * 
     * @param variableName name of the variable
     * @return variable value if present otherwise null
     */
    Object getVariable(String variableName);

    /**
     * Sets variable with given name and value
     * 
     * @param variableName name of the variable
     * @param value value of the variable
     */
    void setVariable(String variableName, Object value);

    /**
     * Returns the time when this node instance was triggered
     * 
     * @return actual trigger time
     */
    Date getTriggerTime();

    /**
     * Returns the time when this node instance was left, might be null if node
     * instance is still active
     * 
     * @return actual leave time
     */
    Date getLeaveTime();

    /**
     * Returns current state of the node instance.
     * 
     * @return current state
     */
    NodeInstanceState getNodeInstanceState();

}
