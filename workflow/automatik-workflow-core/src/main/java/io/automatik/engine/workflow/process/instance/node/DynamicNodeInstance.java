
package io.automatik.engine.workflow.process.instance.node;

import static io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE;
import static io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl.EVENT_NODE_ENTER;
import static io.automatik.engine.workflow.process.executable.core.Metadata.IS_FOR_COMPENSATION;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.DynamicNode;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;

public class DynamicNodeInstance extends CompositeContextNodeInstance {

    private static final long serialVersionUID = 510l;

    private String getRuleFlowGroupName() {
        return getNodeName();
    }

    protected DynamicNode getDynamicNode() {
        return (DynamicNode) getNode();
    }

    @Override
    public String getNodeName() {
        return resolveVariable(super.getNodeName());
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        triggerTime = new Date();
        triggerEvent(EVENT_NODE_ENTER);

        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        if (canActivate()) {
            triggerActivated();
        } else {
            setState(ProcessInstance.STATE_PENDING);
            addActivationListener();
        }
    }

    private void triggerActivated() {
        setState(ProcessInstance.STATE_ACTIVE);
        addCompletionListeners();
        // activate ad hoc fragments if they are marked as such
        List<Node> autoStartNodes = getDynamicNode().getAutoStartNodes();
        autoStartNodes.forEach(autoStartNode -> triggerSelectedNode(autoStartNode, null));
    }

    private boolean canActivate() {
        ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime()).setNodeInstance(this);
        return getDynamicNode().canActivate(context);
    }

    private boolean canComplete() {
        ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime()).setNodeInstance(this);
        return getNodeInstances(false).isEmpty() && getDynamicNode().canComplete(context);
    }

    private void addActivationListener() {
        getProcessInstance().addEventListener("variableChanged", this, false);
    }

    private void addCompletionListener() {
        getProcessInstance().addEventListener("variableChanged", this, false);
    }

    @Override
    public void removeEventListeners() {
        super.removeEventListeners();
    }

    @Override
    public void nodeInstanceCompleted(io.automatik.engine.workflow.process.instance.NodeInstance nodeInstance,
            String outType) {
        Node nodeInstanceNode = nodeInstance.getNode();
        if (nodeInstanceNode != null) {
            Object compensationBoolObj = nodeInstanceNode.getMetaData().get(IS_FOR_COMPENSATION);
            if (Boolean.TRUE.equals(compensationBoolObj)) {
                return;
            }
        }
        // TODO what if we reach the end of one branch but others might still need to be
        // created ?
        // TODO are we sure there will always be node instances left if we are not done
        // yet?
        if (isTerminated(nodeInstance) || canComplete()) {
            triggerCompleted(CONNECTION_DEFAULT_TYPE);
        }
        if (!canComplete()) {
            addCompletionListener();
        }
    }

    @Override
    public void triggerCompleted(String outType) {
        super.triggerCompleted(outType);
    }

    protected boolean isTerminated(NodeInstance from) {
        if (from instanceof EndNodeInstance) {
            return ((EndNodeInstance) from).getEndNode().isTerminate();
        }
        return false;
    }

    public void signalEvent(String type, Object event) {
        if ("variableChanged".equals(type)) {
            if (canActivate() && getState() == ProcessInstance.STATE_PENDING) {
                getProcessInstance().removeEventListener("variableChanged", this, false);
                triggerActivated();
            } else if (canComplete()) {
                triggerCompleted(CONNECTION_DEFAULT_TYPE);
            }
        }
        List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
        if (exitEvents != null && exitEvents.contains(type)) {
            boolean hasCondition = exitOnCompletionCondition();
            if (!hasCondition) {
                cancel();
            }
        }
        for (Node node : getCompositeNode().getNodes()) {

            if ((node.hasMatchingEventListner(type) || type.equals(resolveVariable(node.getName())))
                    && node.getIncomingConnections().isEmpty()) {
                triggerSelectedNode(node, event);
            }
        }

    }

    @SuppressWarnings("unchecked")
    protected void triggerSelectedNode(Node node, Object event) {
        io.automatik.engine.workflow.process.instance.NodeInstance nodeInstance = getNodeInstance(node);
        if (nodeInstance != null) {
            if (event != null) {
                Map<String, Object> dynamicParams = new HashMap<>();
                if (event instanceof Map) {
                    dynamicParams.putAll((Map<String, Object>) event);
                } else if (event instanceof WorkflowProcessInstance) {
                    // ignore variables of process instance type
                } else {
                    dynamicParams.put("Data", event);
                }
                nodeInstance.setDynamicParameters(dynamicParams);
            }
            nodeInstance.trigger(null, CONNECTION_DEFAULT_TYPE);
        }
    }

    @SuppressWarnings("unchecked")
    private void addCompletionListeners() {
        List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
        if (exitEvents != null) {

            for (String event : exitEvents) {
                getProcessInstance().addEventListener(event, this, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeCompletionListeners() {
        List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
        if (exitEvents != null) {

            for (String event : exitEvents) {
                getProcessInstance().removeEventListener(event, this, false);
            }
        }
    }

}
