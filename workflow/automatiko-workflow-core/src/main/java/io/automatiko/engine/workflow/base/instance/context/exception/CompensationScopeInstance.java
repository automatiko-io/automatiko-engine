
package io.automatiko.engine.workflow.base.instance.context.exception;

import static io.automatiko.engine.workflow.base.core.context.exception.CompensationScope.IMPLICIT_COMPENSATION_PREFIX;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationHandler;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.EventNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventSubProcessNodeInstance;

public class CompensationScopeInstance extends ExceptionScopeInstance {

    private static final long serialVersionUID = 510l;

    private Stack<NodeInstance> compensationInstances = new Stack<NodeInstance>();

    public String getContextType() {
        return CompensationScope.COMPENSATION_SCOPE;
    }

    public void addCompensationInstances(Collection<NodeInstance> generatedInstances) {
        this.compensationInstances.addAll(generatedInstances);
    }

    public void handleException(io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance, String activityRef,
            Object dunno) {
        assert activityRef != null : "It should not be possible for the compensation activity reference to be null here.";

        CompensationScope compensationScope = (CompensationScope) getExceptionScope();
        // broadcast/general compensation in reverse order
        if (activityRef.startsWith(IMPLICIT_COMPENSATION_PREFIX)) {
            activityRef = activityRef.substring(IMPLICIT_COMPENSATION_PREFIX.length());
            assert activityRef.equals(compensationScope.getContextContainerId()) : "Compensation activity ref ["
                    + activityRef + "] does not match" + " Compensation Scope container id ["
                    + compensationScope.getContextContainerId() + "]";

            Map<String, ExceptionHandler> handlers = compensationScope.getExceptionHandlers();
            List<String> completedNodeIds = ((WorkflowProcessInstanceImpl) getProcessInstance()).getCompletedNodeIds();
            ListIterator<String> iter = completedNodeIds.listIterator(completedNodeIds.size());
            while (iter.hasPrevious()) {
                String completedId = iter.previous();
                ExceptionHandler handler = handlers.get(completedId);
                if (handler != null) {
                    handleException(nodeInstance, handler, completedId, null);
                }
            }
        } else {
            // Specific compensation
            ExceptionHandler handler = compensationScope.getExceptionHandler(activityRef);
            if (handler == null) {
                throw new IllegalArgumentException("Could not find CompensationHandler for " + activityRef);
            }
            handleException(nodeInstance, handler, activityRef, null);
        }

        // Cancel all node instances created for compensation
        while (!compensationInstances.isEmpty()) {
            NodeInstance generatedInstance = compensationInstances.pop();
            ((NodeInstanceContainer) generatedInstance.getNodeInstanceContainer())
                    .removeNodeInstance(generatedInstance);
        }
    }

    public void handleException(io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance, ExceptionHandler handler,
            String compensationActivityRef, Object dunno) {
        WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) getProcessInstance();
        NodeInstanceContainer nodeInstanceContainer = (NodeInstanceContainer) getContextInstanceContainer();
        if (handler instanceof CompensationHandler) {
            CompensationHandler compensationHandler = (CompensationHandler) handler;
            try {
                Node handlerNode = compensationHandler.getnode();
                if (handlerNode instanceof BoundaryEventNode) {
                    NodeInstance compensationHandlerNodeInstance = nodeInstanceContainer.getNodeInstance(handlerNode);
                    compensationInstances.add(compensationHandlerNodeInstance);
                    // The BoundaryEventNodeInstance.signalEvent() contains the necessary logic
                    // to check whether or not compensation may proceed (? : (not-active +
                    // completed))
                    EventNodeInstance eventNodeInstance = (EventNodeInstance) compensationHandlerNodeInstance;
                    eventNodeInstance.signalEvent("Compensation", compensationActivityRef);
                } else if (handlerNode instanceof EventSubProcessNode) {
                    // Check that subprocess parent has completed.
                    List<String> completedIds = processInstance.getCompletedNodeIds();
                    if (completedIds.contains(((NodeImpl) handlerNode.getParentContainer()).getMetaData("UniqueId"))) {
                        NodeInstance subProcessNodeInstance = ((NodeInstanceContainer) nodeInstanceContainer)
                                .getNodeInstance((Node) handlerNode.getParentContainer());
                        compensationInstances.add(subProcessNodeInstance);
                        NodeInstance compensationHandlerNodeInstance = ((NodeInstanceContainer) subProcessNodeInstance)
                                .getNodeInstance(handlerNode);
                        compensationInstances.add(compensationHandlerNodeInstance);
                        EventSubProcessNodeInstance eventNodeInstance = (EventSubProcessNodeInstance) compensationHandlerNodeInstance;
                        eventNodeInstance.signalEvent("Compensation", compensationActivityRef);
                    }
                }
                assert handlerNode instanceof BoundaryEventNode
                        || handlerNode instanceof EventSubProcessNode : "Unexpected compensation handler node type : "
                                + handlerNode.getClass().getSimpleName();
            } catch (Exception e) {
                throwWorkflowRuntimeException(nodeInstanceContainer, processInstance, "Unable to execute compensation.",
                        e);
            }
        } else {
            Exception e = new IllegalArgumentException("Unsupported compensation handler: " + handler);
            throwWorkflowRuntimeException(nodeInstanceContainer, processInstance, e.getMessage(), e);
        }
    }

    private void throwWorkflowRuntimeException(NodeInstanceContainer nodeInstanceContainer,
            ProcessInstance processInstance, String msg, Exception e) {
        if (nodeInstanceContainer instanceof NodeInstance) {
            throw new WorkflowRuntimeException(
                    (io.automatiko.engine.api.runtime.process.NodeInstance) nodeInstanceContainer, processInstance, msg,
                    e);
        } else {
            throw new WorkflowRuntimeException(null, processInstance, msg, e);
        }
    }
}
