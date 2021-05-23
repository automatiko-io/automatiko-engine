
package io.automatiko.engine.workflow.process.instance.node;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatiko.engine.workflow.process.core.node.EndNode.PROCESS_SCOPE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.HIDDEN;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.impl.ExtendedNodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

/**
 * Runtime counterpart of an end node.
 */
public class EndNodeInstance extends ExtendedNodeInstanceImpl {

    private static final long serialVersionUID = 510l;

    public EndNode getEndNode() {
        return (EndNode) getNode();
    }

    public void internalTrigger(final NodeInstance from, String type) {
        super.internalTrigger(from, type);
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("An EndNode only accepts default incoming connections!");
        }
        leaveTime = new Date();
        boolean hidden = false;
        if (getNode().getMetaData().get(HIDDEN) != null) {
            hidden = true;
        }

        if (getProcessInstance().isFunctionFlow(this) && getNodeInstanceContainer() instanceof ProcessInstance) {
            // only when running as function flow and node is in the top level node container meaning process instance
            // and not subprocesses
            getProcessInstance().getMetaData().compute("ATK_FUNC_FLOW_NEXT", (k, v) -> {

                if (v == null) {
                    v = new ArrayList<String>();
                }
                Process process = getProcessInstance().getProcess();
                String defaultNextNode = process.getPackageName() + "." + process.getId() + "." + getNodeName().toLowerCase();

                ((List<String>) v).add((String) getNode().getMetaData().getOrDefault("functionType", defaultNextNode));

                return v;
            });
        }

        InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
        if (!hidden) {
            runtime.getProcessEventSupport().fireBeforeNodeLeft(this, runtime);
        }
        ((NodeInstanceContainer) getNodeInstanceContainer()).removeNodeInstance(this);
        if (getEndNode().isTerminate()) {
            if (getNodeInstanceContainer() instanceof CompositeNodeInstance) {
                if (getEndNode().getScope() == PROCESS_SCOPE) {
                    getProcessInstance().setState(STATE_COMPLETED);
                } else {
                    while (!getNodeInstanceContainer().getNodeInstances().isEmpty()) {
                        ((io.automatiko.engine.workflow.process.instance.NodeInstance) getNodeInstanceContainer()
                                .getNodeInstances().iterator().next()).cancel();
                    }
                    ((NodeInstanceContainer) getNodeInstanceContainer()).nodeInstanceCompleted(this, null);
                }
            } else {
                ((NodeInstanceContainer) getNodeInstanceContainer()).setState(STATE_COMPLETED);
            }

        } else {
            ((NodeInstanceContainer) getNodeInstanceContainer()).nodeInstanceCompleted(this, null);
        }
        if (!hidden) {
            runtime.getProcessEventSupport().fireAfterNodeLeft(this, runtime);
        }
        String uniqueId = (String) getNode().getMetaData().get(UNIQUE_ID);
        if (uniqueId == null) {
            uniqueId = ((NodeImpl) getNode()).getUniqueId();
        }
        ((WorkflowProcessInstanceImpl) getProcessInstance()).addCompletedNodeId(uniqueId);
    }

}
