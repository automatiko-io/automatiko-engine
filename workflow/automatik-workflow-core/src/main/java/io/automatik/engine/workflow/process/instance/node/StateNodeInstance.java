
package io.automatik.engine.workflow.process.instance.node;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.event.process.ContextAwareEventListener;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.process.core.Constraint;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstance;

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
        getProcessInstance().getProcessRuntime().addEventListener(ContextAwareEventListener.using(getId(), listener -> {
            if (isCompleted()) {
                getProcessInstance().getProcessRuntime().removeEventListener(listener);
                listener.deactivate();
                triggerCompleted();
            }
        }));
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
                        ((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
                                .removeNodeInstance(this);
                        triggerConnection(connection);
                        return;
                    }
                }
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
        if (getProcessInstance().getProcessRuntime() != null) {
            ContextAwareEventListener listener = (ContextAwareEventListener) ContextAwareEventListener.using(getId(), null);
            listener.deactivate();
            getProcessInstance().getProcessRuntime().removeEventListener(listener);

        }
    }

    public String[] getEventTypes() {
        return new String[] { "signal" };
    }

}
