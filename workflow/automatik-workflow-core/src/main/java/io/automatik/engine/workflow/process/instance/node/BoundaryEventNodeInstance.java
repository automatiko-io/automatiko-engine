package io.automatik.engine.workflow.process.instance.node;

import java.util.Collection;

import io.automatik.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class BoundaryEventNodeInstance extends EventNodeInstance {

    private static final long serialVersionUID = -4958054074031174180L;

    @Override
    public void signalEvent(String type, Object event) {
        BoundaryEventNode boundaryNode = (BoundaryEventNode) getEventNode();

        String attachedTo = boundaryNode.getAttachedToNodeId();
        Collection<NodeInstance> nodeInstances = ((NodeInstanceContainer) getProcessInstance()).getNodeInstances(true);
        if (type != null && type.startsWith("Compensation")) {
            // if not active && completed, signal
            if (!isAttachedToNodeActive(nodeInstances, attachedTo, type, event)
                    && isAttachedToNodeCompleted(attachedTo)) {
                super.signalEvent(type, event);
            } else {
                cancel();
            }
        } else {
            if (isAttachedToNodeActive(nodeInstances, attachedTo, type, event)) {
                super.signalEvent(type, event);
            } else {
                cancel();
            }
        }
    }

    private boolean isAttachedToNodeActive(Collection<NodeInstance> nodeInstances, String attachedTo, String type,
            Object event) {
        if (nodeInstances != null && !nodeInstances.isEmpty()) {
            for (NodeInstance nInstance : nodeInstances) {
                String nodeUniqueId = (String) nInstance.getNode().getMetaData().get("UniqueId");
                boolean isActivating = ((WorkflowProcessInstanceImpl) nInstance.getProcessInstance())
                        .getActivatingNodeIds().contains(nodeUniqueId);
                if (attachedTo.equals(nodeUniqueId) && !isActivating) {
                    // in case this is timer event make sure it corresponds to the proper node
                    // instance
                    if (type.startsWith("Timer-")) {
                        if (nInstance.getId().equals(event)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAttachedToNodeCompleted(String attachedTo) {
        WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) getProcessInstance();
        return processInstance.getCompletedNodeIds().contains(attachedTo);
    }

    @Override
    public void cancel() {
        getProcessInstance().removeEventListener(getEventType(), getEventListener(), true);
        ((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
    }
}
