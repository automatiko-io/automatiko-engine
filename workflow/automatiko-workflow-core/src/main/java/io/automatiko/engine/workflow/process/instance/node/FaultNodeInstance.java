
package io.automatiko.engine.workflow.process.instance.node;

import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.instance.NodeInstanceContainer;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;

/**
 * Runtime counterpart of a fault node.
 * 
 */
public class FaultNodeInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(FaultNodeInstance.class);

    protected FaultNode getFaultNode() {
        return (FaultNode) getNode();
    }

    public void internalTrigger(final NodeInstance from, String type) {
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A FaultNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        if (getProcessInstance().isFunctionFlow()) {
            getProcessInstance().getMetaData().put("ATK_FUNC_FLOW_NEXT", getNodeName());
        }
        String faultName = getFaultName();
        ExceptionScopeInstance exceptionScopeInstance = getExceptionScopeInstance(faultName);
        NodeInstanceContainer nodeInstanceContainer = (NodeInstanceContainer) getNodeInstanceContainer();
        nodeInstanceContainer.removeNodeInstance(this);
        boolean exceptionHandled = false;
        if (getFaultNode().isTerminateParent()) {
            // handle exception before canceling nodes to allow boundary event to catch the
            // events
            if (exceptionScopeInstance != null) {
                exceptionHandled = true;
                handleException(faultName, exceptionScopeInstance);
            }
            if (nodeInstanceContainer instanceof CompositeNodeInstance) {

                ((CompositeNodeInstance) nodeInstanceContainer).cancel();
            } else if (nodeInstanceContainer instanceof WorkflowProcessInstance) {
                Collection<NodeInstance> nodeInstances = ((WorkflowProcessInstance) nodeInstanceContainer)
                        .getNodeInstances();
                for (NodeInstance nodeInstance : nodeInstances) {
                    ((io.automatiko.engine.workflow.process.instance.NodeInstance) nodeInstance).cancel();
                }
            }
        }
        if (exceptionScopeInstance != null) {
            if (!exceptionHandled) {
                handleException(faultName, exceptionScopeInstance);
            }
            boolean hidden = false;
            if (getNode().getMetaData().get("hidden") != null) {
                hidden = true;
            }
            if (!hidden) {
                InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
                runtime.getProcessEventSupport().fireBeforeNodeLeft(this, runtime);
            }

            ((NodeInstanceContainer) getNodeInstanceContainer()).nodeInstanceCompleted(this, null);

            if (!hidden) {
                InternalProcessRuntime runtime = getProcessInstance().getProcessRuntime();
                runtime.getProcessEventSupport().fireAfterNodeLeft(this, runtime);
            }
        } else {

            ((ProcessInstance) getProcessInstance()).setState(ProcessInstance.STATE_ABORTED, faultName, getFaultData());

        }
    }

    protected ExceptionScopeInstance getExceptionScopeInstance(String faultName) {
        return (ExceptionScopeInstance) resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, faultName);
    }

    protected String getFaultName() {
        return getFaultNode().getFaultName();
    }

    protected Object getFaultData() {
        Object value = null;
        String faultVariable = getFaultNode().getFaultVariable();
        if (faultVariable != null) {
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                    VariableScope.VARIABLE_SCOPE, faultVariable);
            if (variableScopeInstance != null) {
                value = variableScopeInstance.getVariable(faultVariable);
            } else {
                logger.error("Could not find variable scope for variable {}", faultVariable);
                logger.error("when trying to execute fault node {}", getFaultNode().getName());
                logger.error("Continuing without setting value.");
            }
        }
        return value;
    }

    protected void handleException(String faultName, ExceptionScopeInstance exceptionScopeInstance) {
        exceptionScopeInstance.handleException(this, faultName, getFaultData());
    }

}
