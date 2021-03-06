
package io.automatiko.engine.workflow.process.instance.impl;

import static io.automatiko.engine.workflow.base.core.context.exception.CompensationScope.COMPENSATION_SCOPE;
import static io.automatiko.engine.workflow.base.core.context.exception.CompensationScope.IMPLICIT_COMPENSATION_PREFIX;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.CompensationScopeInstance;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatiko.engine.workflow.process.instance.node.CompositeNodeInstance;

class CompensationEventListener implements EventListener {

    private WorkflowProcessInstanceImpl instance;

    public CompensationEventListener(WorkflowProcessInstanceImpl instance) {
        this.instance = instance;
    }

    private ProcessInstance getProcessInstance() {
        return instance;
    }

    /**
     * When signaling compensation, you can do that in 1 of 2 ways: 1.
     * signalEvent("Compensation", <node-with-compensation-handler-id>) This is
     * specific compensation, that only possibly triggers the compensation handler
     * attached to the node referred to by the <node-with-compensation-handler-id>.
     * 2. signalEvent("Compensation", "implicit:" +
     * <node-container-containing-compensation-scope-id> ) This is implicit or
     * general compensation, in which you trigger all visible compensation handlers
     * (in the proper order, etc.) in the (sub-)process referred to by the
     * <node-container-containing-compensation-scope-id>.
     */
    public void signalEvent(String compensationType, Object activityRefStr) {
        if (!(activityRefStr instanceof String)) {
            throw new WorkflowRuntimeException(null, getProcessInstance(),
                    "Compensation can only be triggered with String events, not an event of type "
                            + (activityRefStr == null ? "null" : activityRefStr.getClass().getSimpleName()));
        }

        // 1. parse the activity ref (is it general or specific compensation?)
        String activityRef = (String) activityRefStr;
        String toCompensateNodeId = activityRef;
        boolean generalCompensation = false;
        if (activityRef.startsWith(IMPLICIT_COMPENSATION_PREFIX)) {
            toCompensateNodeId = activityRef.substring(IMPLICIT_COMPENSATION_PREFIX.length());
            generalCompensation = true;
        }

        io.automatiko.engine.workflow.base.core.Process process = (io.automatiko.engine.workflow.base.core.Process) instance
                .getProcess();

        // 2. for specific compensation: find the node that will be compensated
        // for general compensation: find the compensation scope container that contains
        // all the visible compensation handlers
        Node toCompensateNode = null;
        ContextContainer compensationScopeContainer = null;
        if (generalCompensation) {
            if (toCompensateNodeId.equals(instance.getProcessId())) {
                compensationScopeContainer = process;
            } else {
                compensationScopeContainer = (ContextContainer) findNode(toCompensateNodeId);
            }
        } else {
            toCompensateNode = findNode(toCompensateNodeId);
        }

        // 3. If the node exists,
        // a. find the node container for which the compensation handler is visible
        // b. create the compensation scope instance
        // c. handle the exception (which also cleans up the generated node instances)
        if (toCompensateNode != null || compensationScopeContainer != null) {
            CompensationScope compensationScope = null;
            if (compensationScopeContainer != null) {
                compensationScope = (CompensationScope) compensationScopeContainer
                        .getDefaultContext(COMPENSATION_SCOPE);
            } else {
                compensationScope = (CompensationScope) ((NodeImpl) toCompensateNode).resolveContext(COMPENSATION_SCOPE,
                        toCompensateNodeId);
            }
            assert compensationScope != null : "Compensation scope for node [" + toCompensateNodeId
                    + "] could not be found!";

            CompensationScopeInstance scopeInstance;
            if (compensationScope.getContextContainerId().equals(process.getId())) {
                // process level compensation
                scopeInstance = (CompensationScopeInstance) instance.getContextInstance(compensationScope);
            } else {
                // nested compensation
                Stack<NodeInstance> generatedInstances;
                if (toCompensateNode == null) {
                    // logic is the same if it's specific or general
                    generatedInstances = createNodeInstanceContainers((Node) compensationScopeContainer, true);
                } else {
                    generatedInstances = createNodeInstanceContainers(toCompensateNode, false);
                }
                NodeInstance nodeInstanceContainer = generatedInstances.peek();
                scopeInstance = ((CompensationScopeInstance) ((ContextInstanceContainer) nodeInstanceContainer)
                        .getContextInstance(compensationScope));
                scopeInstance.addCompensationInstances(generatedInstances);
            }

            scopeInstance.handleException(null, activityRef, null);
        }
    }

    private Node findNode(String nodeId) {
        Node found = null;
        Queue<Node> allProcessNodes = new LinkedList<>();
        allProcessNodes.addAll(Arrays.asList(instance.getNodeContainer().getNodes()));
        while (!allProcessNodes.isEmpty()) {
            Node node = allProcessNodes.poll();
            if (nodeId.equals(node.getMetaData().get("UniqueId"))) {
                found = node;
                break;
            }
            if (node instanceof NodeContainer) {
                allProcessNodes.addAll(Arrays.asList(((NodeContainer) node).getNodes()));
            }
        }
        return found;
    }

    private Stack<NodeInstance> createNodeInstanceContainers(Node toCompensateNode, boolean generalCompensation) {
        Stack<NodeContainer> nestedNodes = new Stack<NodeContainer>();
        Stack<NodeInstance> generatedInstances = new Stack<NodeInstance>();

        NodeContainer parentContainer = toCompensateNode.getParentContainer();
        while (!(parentContainer instanceof ExecutableProcess)) {
            nestedNodes.add(parentContainer);
            parentContainer = ((Node) parentContainer).getParentContainer();
        }

        NodeInstanceContainer parentInstance;

        if (nestedNodes.isEmpty()) {
            // nestedNodes is empty
            parentInstance = (NodeInstanceContainer) getProcessInstance();
        } else {
            parentInstance = (NodeInstanceContainer) ((WorkflowProcessInstanceImpl) getProcessInstance())
                    .getNodeInstance((Node) nestedNodes.pop());
            generatedInstances.add((NodeInstance) parentInstance);
        }

        NodeInstanceContainer childInstance;
        while (!nestedNodes.isEmpty()) {
            // generate
            childInstance = (NodeInstanceContainer) parentInstance.getNodeInstance((Node) nestedNodes.pop());
            assert childInstance instanceof CompositeNodeInstance : "A node with child nodes should end up creating a CompositeNodeInstance type.";

            // track and modify
            generatedInstances.add((NodeInstance) childInstance);

            // loop
            parentInstance = childInstance;
        }
        if (generalCompensation) {
            childInstance = (NodeInstanceContainer) parentInstance.getNodeInstance(toCompensateNode);
            generatedInstances.add((NodeInstance) childInstance);
        }

        return generatedInstances;
    }

    private final String[] eventTypes = { "Compensation" };

    public String[] getEventTypes() {
        return eventTypes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instance == null) ? 0 : instance.getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CompensationEventListener other = (CompensationEventListener) obj;
        if (instance == null) {
            if (other.instance != null)
                return false;
        } else if (!instance.getId().equals(other.instance.getId()))
            return false;
        return true;
    }

}