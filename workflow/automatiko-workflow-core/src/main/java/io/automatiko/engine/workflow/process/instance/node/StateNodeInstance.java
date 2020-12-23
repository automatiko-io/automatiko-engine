
package io.automatiko.engine.workflow.process.instance.node;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;

public class StateNodeInstance extends CompositeContextNodeInstance implements EventListener {

    private static final long serialVersionUID = 510l;

    protected StateNode getStateNode() {
        return (StateNode) getNode();
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        if (isCompleted()) {
            triggerCompleted();
        } else {
            addCompletionEventListener();
            exitOnCompletionCondition();
        }
    }

    protected boolean isLinkedIncomingNodeRequired() {
        return false;
    }

    private boolean isCompleted() {
        if (getProcessInstance() instanceof ExecutableProcessInstance) {

            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
            context.setProcessInstance(getProcessInstance());
            context.setNodeInstance(this);
            return getStateNode().isMet(context);
        } else {
            if (((Node) getNode()).getCompletionCheck().isPresent()) {

                if (((Node) getNode()).getCompletionCheck().get()
                        .isValid(getProcessInstance().getVariables())) {
                    return true;
                }
            }

            return false;
        }

    }

    private void addCompletionEventListener() {
        getProcessInstance().addEventListener("variableChanged", this, false);
    }

    public void signalEvent(String type, Object event) {
        if ("signal".equals(type)) {
            if (event instanceof String) {
                for (Connection connection : getStateNode().getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE)) {
                    boolean selected = false;
                    Constraint constraint = getStateNode().getConstraint(connection);
                    if (constraint == null) {
                        if (((String) event).equals(connection.getTo().getName())) {
                            selected = true;
                        }
                    } else if (((String) event).equals(constraint.getName())) {
                        selected = true;
                    }
                    if (selected) {
                        triggerEvent(ExtendedNodeImpl.EVENT_NODE_EXIT);
                        removeEventListeners();
                        ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
                                .removeNodeInstance(this);
                        triggerConnection(connection);
                        return;
                    }
                }
            }
        } else if ("variableChanged".equals(type)) {
            if (isCompleted()) {
                triggerCompleted();
            }
        } else {
            super.signalEvent(type, event);
        }
    }

    private void addTriggerListener() {
        getProcessInstance().addEventListener("signal", this, false);
    }

    public void addEventListeners() {
        super.addEventListeners();
        addTriggerListener();
        addCompletionEventListener();
    }

    public void removeEventListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener("signal", this, false);
        getProcessInstance().removeEventListener("variableChanged", this, false);
    }

    public String[] getEventTypes() {
        return new String[] { "signal" };
    }

}
