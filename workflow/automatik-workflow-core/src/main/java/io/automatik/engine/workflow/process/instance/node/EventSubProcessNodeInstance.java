package io.automatik.engine.workflow.process.instance.node;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.instance.NodeInstanceContainer;

public class EventSubProcessNodeInstance extends CompositeContextNodeInstance {

    private static final long serialVersionUID = 7095736653568661510L;

    protected EventSubProcessNode getCompositeNode() {
        return (EventSubProcessNode) getNode();
    }

    public NodeContainer getNodeContainer() {
        return getCompositeNode();
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        super.internalTriggerOnlyParent(from, type);
    }

    @Override
    public void signalEvent(String type, Object event) {
        if (triggerTime == null) {
            // started by signal
            triggerTime = new Date();
        }

        if (getNodeInstanceContainer().getNodeInstances().contains(this) || type.startsWith("Error-")
                || type.equals("timerTriggered")) {
            // start it only if it was not already started - meaning there are node
            // instances
            if (this.getNodeInstances().isEmpty()) {
                StartNode startNode = getCompositeNode().findStartNode();
                if (resolveVariables(((EventSubProcessNode) getEventBasedNode()).getEvents()).contains(type)
                        || type.equals("timerTriggered")) {
                    NodeInstance nodeInstance = getNodeInstance(startNode);
                    ((StartNodeInstance) nodeInstance).signalEvent(type, event);

                    return;
                }
            }
        }
        super.signalEvent(type, event);
    }

    @Override
    public void nodeInstanceCompleted(io.automatik.engine.workflow.process.instance.NodeInstance nodeInstance,
            String outType) {
        if (nodeInstance instanceof EndNodeInstance) {
            if (getCompositeNode().isKeepActive()) {
                StartNode startNode = getCompositeNode().findStartNode();
                triggerCompleted(true);
                if (startNode.isInterrupting()) {
                    String faultName = getProcessInstance().getOutcome() == null ? ""
                            : getProcessInstance().getOutcome();

                    if (startNode.getMetaData("FaultCode") != null) {
                        faultName = (String) startNode.getMetaData("FaultCode");
                    }
                    if (getNodeInstanceContainer() instanceof ProcessInstance) {
                        ((ProcessInstance) getProcessInstance()).setState(ProcessInstance.STATE_ABORTED, faultName);
                    } else {
                        ((NodeInstanceContainer) getNodeInstanceContainer()).setState(ProcessInstance.STATE_ABORTED);
                    }

                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Completing a node instance that has no outgoing connection not supported.");
        }
    }

    protected List<String> resolveVariables(List<String> events) {
        return events.stream().map(event -> resolveVariable(event)).collect(Collectors.toList());
    }
}
