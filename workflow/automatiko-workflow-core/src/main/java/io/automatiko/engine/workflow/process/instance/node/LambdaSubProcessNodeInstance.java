
package io.automatiko.engine.workflow.process.instance.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.SubProcessFactory;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;

/**
 * Runtime counterpart of a SubFlow node.
 * 
 */
public class LambdaSubProcessNodeInstance extends StateBasedNodeInstance
        implements EventListener, ContextInstanceContainer {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(LambdaSubProcessNodeInstance.class);

    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<>();

    private String processInstanceId;

    private String processInstanceName;

    protected SubProcessNode getSubProcessNode() {
        return (SubProcessNode) getNode();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void internalTrigger(final NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A SubProcess node only accepts default incoming connections!");
        }

        ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
        context.setNodeInstance(this);
        context.setProcessInstance(getProcessInstance());
        SubProcessFactory subProcessFactory = getSubProcessNode().getSubProcessFactory();
        Object o = subProcessFactory.bind(context);
        io.automatiko.engine.api.workflow.ProcessInstance<?> processInstance = subProcessFactory.createInstance(o);

        io.automatiko.engine.api.runtime.process.ProcessInstance pi = ((AbstractProcessInstance<?>) processInstance)
                .internalGetProcessInstance();
        String parentInstanceId = getProcessInstance().getId();
        if (getProcessInstance().getParentProcessInstanceId() != null
                && !getProcessInstance().getParentProcessInstanceId().isEmpty()) {
            parentInstanceId = getProcessInstance().getParentProcessInstanceId() + ":" + parentInstanceId;
        }
        ((ProcessInstanceImpl) pi).setMetaData("ParentProcessInstanceId", parentInstanceId);
        ((ProcessInstanceImpl) pi).setMetaData("ParentNodeInstanceId", getUniqueId());
        ((ProcessInstanceImpl) pi).setMetaData("ParentNodeId", getSubProcessNode().getUniqueId());
        ((ProcessInstanceImpl) pi).setParentProcessInstanceId(parentInstanceId);
        ((ProcessInstanceImpl) pi).setRootProcessInstanceId(
                StringUtils.isEmpty(getProcessInstance().getRootProcessInstanceId()) ? getProcessInstance().getId()
                        : getProcessInstance().getRootProcessInstanceId());
        ((ProcessInstanceImpl) pi).setRootProcessId(
                StringUtils.isEmpty(getProcessInstance().getRootProcessId()) ? getProcessInstance().getProcessId()
                        : getProcessInstance().getRootProcessId());
        ((ProcessInstanceImpl) pi).setSignalCompletion(getSubProcessNode().isWaitForCompletion());
        ((ProcessInstanceImpl) pi)
                .setReferenceFromRoot(getProcessInstance().getReferenceFromRoot());

        processInstance.start();
        this.processInstanceId = processInstance.id();
        this.processInstanceName = processInstance.description();

        subProcessFactory.unbind(context, processInstance.variables());

        if (!getSubProcessNode().isWaitForCompletion()) {
            triggerCompleted();
        } else if (processInstance.status() == ProcessInstance.STATE_COMPLETED
                || processInstance.status() == ProcessInstance.STATE_ABORTED) {
            triggerCompleted();
        } else {
            String subprocessInstanceId = parentInstanceId + ":" + processInstance.id();

            ((ProcessInstanceImpl) getProcessInstance()).addChild(processInstance.process().id(), subprocessInstanceId);
            addProcessListener();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (getSubProcessNode() == null || !getSubProcessNode().isIndependent()) {
            SubProcessFactory<?> subProcessFactory = getSubProcessNode().getSubProcessFactory();
            subProcessFactory.abortInstance(getProcessInstance().getId() + ":" + getProcessInstanceId());

        }
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void internalSetProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void internalSetProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public void addEventListeners() {
        super.addEventListeners();
        addProcessListener();
    }

    private void addProcessListener() {
        getProcessInstance().addEventListener("processInstanceCompleted:" + processInstanceId, this, true);
    }

    public void removeEventListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener("processInstanceCompleted:" + processInstanceId, this, true);
    }

    @Override
    public void signalEvent(String type, Object event) {
        if (("processInstanceCompleted:" + processInstanceId).equals(type)) {
            processInstanceCompleted((ProcessInstance) event);
        } else {
            super.signalEvent(type, event);
        }
    }

    @Override
    public String[] getEventTypes() {
        return new String[] { "processInstanceCompleted:" + processInstanceId };
    }

    public void processInstanceCompleted(ProcessInstance processInstance) {
        removeEventListeners();
        String parentInstanceId = getProcessInstance().getId();
        if (getProcessInstance().getParentProcessInstanceId() != null
                && !getProcessInstance().getParentProcessInstanceId().isEmpty()) {
            parentInstanceId = getProcessInstance().getParentProcessInstanceId() + ":" + parentInstanceId;
        }
        ((ProcessInstanceImpl) getProcessInstance()).removeChild(processInstance.getProcess().getId(),
                parentInstanceId + ":" + processInstance.getId());
        handleOutMappings(processInstance);
        if (processInstance.getState() == ProcessInstance.STATE_ABORTED) {
            String faultName = processInstance.getOutcome() == null ? "" : processInstance.getOutcome();
            // handle exception as sub process failed with error code
            ExceptionScopeInstance exceptionScopeInstance = (ExceptionScopeInstance) resolveContextInstance(
                    ExceptionScope.EXCEPTION_SCOPE, faultName);
            if (exceptionScopeInstance != null) {

                exceptionScopeInstance.handleException(this, faultName, processInstance.getFaultData());
                if (getSubProcessNode() != null && !getSubProcessNode().isIndependent()
                        && getSubProcessNode().isAbortParent()) {
                    cancel();
                }
                return;
            } else if (getSubProcessNode() != null && !getSubProcessNode().isIndependent()
                    && getSubProcessNode().isAbortParent()) {
                getProcessInstance().setState(ProcessInstance.STATE_ABORTED, faultName);
                return;
            }

        }
        // handle dynamic subprocess
        if (getNode() == null) {
            setMetaData("NodeType", "SubProcessNode");
        }
        // if there were no exception proceed normally
        triggerCompleted();

        if (getProcessInstance().getProcess().getType().equals(Process.FUNCTION_FLOW_TYPE)) {
            processInstance.getMetaData().put("ATK_FUNC_FLOW_NEXT",
                    getProcessInstance().getMetaData().get("ATK_FUNC_FLOW_NEXT"));
            processInstance.getMetaData().put("ATK_FUNC_FLOW_ID", getProcessInstance().getId());
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void handleOutMappings(ProcessInstance processInstance) {

        SubProcessFactory subProcessFactory = getSubProcessNode().getSubProcessFactory();
        ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
        context.setNodeInstance(this);
        context.setProcessInstance(getProcessInstance());
        io.automatiko.engine.api.workflow.ProcessInstance<?> pi = ((io.automatiko.engine.api.workflow.ProcessInstance<?>) processInstance
                .getMetaData().get("AutomatikProcessInstance"));
        if (pi != null) {
            subProcessFactory.unbind(context, pi.variables());
        }
    }

    public String getNodeName() {
        Node node = getNode();
        if (node == null) {
            return "[Dynamic] Sub Process";
        }
        return super.getNodeName();
    }

    @Override
    public List<ContextInstance> getContextInstances(String contextId) {
        return this.subContextInstances.get(contextId);
    }

    @Override
    public void addContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list == null) {
            list = new ArrayList<ContextInstance>();
            this.subContextInstances.put(contextId, list);
        }
        list.add(contextInstance);
    }

    @Override
    public void removeContextInstance(String contextId, ContextInstance contextInstance) {
        List<ContextInstance> list = this.subContextInstances.get(contextId);
        if (list != null) {
            list.remove(contextInstance);
        }
    }

    @Override
    public ContextInstance getContextInstance(String contextId, long id) {
        List<ContextInstance> contextInstances = subContextInstances.get(contextId);
        if (contextInstances != null) {
            for (ContextInstance contextInstance : contextInstances) {
                if (contextInstance.getContextId() == id) {
                    return contextInstance;
                }
            }
        }
        return null;
    }

    @Override
    public ContextInstance getContextInstance(Context context) {
        ContextInstanceFactory conf = ContextInstanceFactoryRegistry.INSTANCE.getContextInstanceFactory(context);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal context type (registry not found): " + context.getClass());
        }
        ContextInstance contextInstance = (ContextInstance) conf.getContextInstance(context, this,
                (ProcessInstance) getProcessInstance());
        if (contextInstance == null) {
            throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
        }
        return contextInstance;
    }

    @Override
    public ContextContainer getContextContainer() {
        return getSubProcessNode();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Map<String, Object> getSourceParameters(DataAssociation association) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (String sourceParam : association.getSources()) {
            Object parameterValue = null;
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                    VariableScope.VARIABLE_SCOPE, sourceParam);
            if (variableScopeInstance != null) {
                parameterValue = variableScopeInstance.getVariable(sourceParam);
            } else {
                try {
                    ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcess) getProcessInstance()
                            .getProcess())
                                    .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);
                    parameterValue = evaluator.evaluate(sourceParam, new NodeInstanceResolverFactory(this));
                } catch (Throwable t) {
                    logger.warn("Could not find variable scope for variable {}", sourceParam);
                }
            }
            if (parameterValue != null) {
                parameters.put(sourceParam, parameterValue);
            }
        }

        return parameters;
    }

}
