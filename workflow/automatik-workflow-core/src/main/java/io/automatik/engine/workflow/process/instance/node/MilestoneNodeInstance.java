
package io.automatik.engine.workflow.process.instance.node;

import io.automatik.engine.api.event.process.ContextAwareEventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceState;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.MilestoneNode;
import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstance;

/**
 * Runtime counterpart of a milestone node.
 */
public class MilestoneNodeInstance extends StateBasedNodeInstance {

    private static final long serialVersionUID = 510L;

    protected MilestoneNode getMilestoneNode() {
        return (MilestoneNode) getNode();
    }

    @Override
    public void internalTrigger(final NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A MilestoneNode only accepts default incoming connections!");
        }
        if (isCompleted()) {
            triggerCompleted();
        } else {
            addCompletionEventListener();
            exitOnCompletionCondition();
        }
    }

    private boolean isCompleted() {
        if (getProcessInstance() instanceof ExecutableProcessInstance) {

            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime()).setNodeInstance(this);
            return getMilestoneNode().isMet(context);
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

    @Override
    public void addEventListeners() {
        super.addEventListeners();
        addCompletionEventListener();
    }

    private void addCompletionEventListener() {
        getProcessInstance().getProcessRuntime().addEventListener(ContextAwareEventListener.using(getId(), listener -> {
            if (isCompleted()) {
                triggerCompleted();
                getProcessInstance().getProcessRuntime().removeEventListener(listener);
            }
        }));
    }

    @Override
    public void removeEventListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener(getActivationEventType(), this, true);
        getProcessInstance().getProcessRuntime().removeEventListener(ContextAwareEventListener.using(getId(), null));
    }

    private String getActivationEventType() {
        return "RuleFlow-Milestone-" + getProcessInstance().getProcessId() + "-" + getMilestoneNode().getUniqueId();
    }

    public boolean exitOnCompletionCondition() {
        if (getProcessInstance() instanceof ExecutableProcessInstance) {
            return true;
        }
        if (((Node) getNode()).getCompletionCheck().isPresent()) {

            if (((Node) getNode()).getCompletionCheck().get()
                    .isValid(getProcessInstance().getVariables())) {
                triggerCompleted();
            }
            return true;
        } else {
            triggerCompleted();
            return false;
        }
    }

    @Override
    public void triggerCompleted() {
        internalChangeState(NodeInstanceState.Occur);
        super.triggerCompleted();
    }

}
