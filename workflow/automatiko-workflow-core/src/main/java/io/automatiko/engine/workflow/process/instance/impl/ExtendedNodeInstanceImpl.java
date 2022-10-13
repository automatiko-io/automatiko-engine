
package io.automatiko.engine.workflow.process.instance.impl;

import static io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE;
import static io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl.EVENT_NODE_ENTER;
import static io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl.EVENT_NODE_EXIT;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.ACTION;

import java.util.Date;
import java.util.List;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;

public abstract class ExtendedNodeInstanceImpl extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;

    public ExtendedNodeImpl getExtendedNode() {
        return (ExtendedNodeImpl) getNode();
    }

    public void internalTrigger(NodeInstance from, String type) {
        triggerTime = new Date();
        triggerEvent(EVENT_NODE_ENTER);
    }

    public void triggerCompleted(boolean remove) {
        triggerCompleted(CONNECTION_DEFAULT_TYPE, remove);
    }

    public void triggerCompleted(String type, boolean remove) {
        triggerEvent(EVENT_NODE_EXIT);
        super.triggerCompleted(type, remove);
    }

    protected void triggerEvent(String type) {
        ExtendedNodeImpl extendedNode = getExtendedNode();
        if (extendedNode == null) {
            return;
        }
        List<ProcessAction> actions = extendedNode.getActions(type);
        if (actions != null) {
            for (ProcessAction processAction : actions) {
                Action action = (Action) processAction.getMetaData(ACTION);
                executeAction(action);
            }
        }
    }

}
