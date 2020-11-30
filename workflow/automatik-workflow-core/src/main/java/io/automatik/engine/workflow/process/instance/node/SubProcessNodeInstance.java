
package io.automatik.engine.workflow.process.instance.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.runtime.process.DataTransformer;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.services.correlation.CorrelationAwareProcessRuntime;
import io.automatik.engine.services.correlation.CorrelationKey;
import io.automatik.engine.services.correlation.StringCorrelationKey;
import io.automatik.engine.services.utils.StringUtils;
import io.automatik.engine.workflow.base.core.Context;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatik.engine.workflow.base.instance.ContextInstance;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatik.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatik.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatik.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatik.engine.workflow.process.core.node.DataAssociation;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.core.node.Transformation;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatik.engine.workflow.process.instance.impl.VariableScopeResolverFactory;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatik.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of a SubFlow node.
 * 
 */
public class SubProcessNodeInstance extends StateBasedNodeInstance implements EventListener, ContextInstanceContainer {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(SubProcessNodeInstance.class);

    // NOTE: ContetxInstances are not persisted as current functionality (exception
    // scope) does not require it
    private Map<String, ContextInstance> contextInstances = new HashMap<String, ContextInstance>();
    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<String, List<ContextInstance>>();

    private String processInstanceId;

    protected SubProcessNode getSubProcessNode() {
        return (SubProcessNode) getNode();
    }

    @Override
    public void internalTrigger(final NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A SubProcess node only accepts default incoming connections!");
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (Iterator<DataAssociation> iterator = getSubProcessNode().getInAssociations().iterator(); iterator
                .hasNext();) {
            DataAssociation mapping = iterator.next();
            Object parameterValue = null;
            if (mapping.getTransformation() != null) {
                Transformation transformation = mapping.getTransformation();
                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                if (transformer != null) {
                    parameterValue = transformer.transform(transformation.getCompiledExpression(),
                            getSourceParameters(mapping));

                }
            } else {

                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, mapping.getSources().get(0));
                if (variableScopeInstance != null) {
                    parameterValue = variableScopeInstance.getVariable(mapping.getSources().get(0));
                } else {
                    try {
                        parameterValue = MVEL.eval(mapping.getSources().get(0), new NodeInstanceResolverFactory(this));
                    } catch (Throwable t) {
                        parameterValue = VariableUtil.resolveVariable(mapping.getSources().get(0), this);
                        if (parameterValue != null) {
                            parameters.put(mapping.getTarget(), parameterValue);
                        } else {
                            logger.error("Could not find variable scope for variable {}", mapping.getSources().get(0));
                            logger.error("when trying to execute SubProcess node {}", getSubProcessNode().getName());
                            logger.error("Continuing without setting parameter.");
                        }
                    }
                }
            }
            if (parameterValue != null) {
                parameters.put(mapping.getTarget(), parameterValue);
            }
        }
        String processId = getSubProcessNode().getProcessId();
        if (processId == null) {
            // if process id is not given try with process name
            processId = getSubProcessNode().getProcessName();
        }
        // resolve processId if necessary
        Map<String, String> replacements = new HashMap<String, String>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(processId);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (replacements.get(paramName) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? "" : variableValue.toString();
                    replacements.put(paramName, variableValueString);
                } else {
                    try {
                        Object variableValue = MVEL.eval(paramName, new NodeInstanceResolverFactory(this));
                        String variableValueString = variableValue == null ? "" : variableValue.toString();
                        replacements.put(paramName, variableValueString);
                    } catch (Throwable t) {
                        logger.error("Could not find variable scope for variable {}", paramName);
                        logger.error("when trying to replace variable in processId for sub process {}", getNodeName());
                        logger.error("Continuing without setting process id.");
                    }
                }
            }
        }
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            processId = processId.replace("#{" + replacement.getKey() + "}", replacement.getValue());
        }
        // start process instance
        Process process = getProcessInstance().getProcessRuntime().getProcess(processId);

        if (process == null) {
            logger.error("Could not find process {}", processId);
            logger.error("Aborting process");
            ((ProcessInstance) getProcessInstance()).setState(ProcessInstance.STATE_ABORTED);
            throw new RuntimeException("Could not find process " + processId);
        } else {
            ProcessRuntime kruntime = ((ProcessInstance) getProcessInstance()).getProcessRuntime();
            if (getSubProcessNode().getMetaData("MICollectionInput") != null) {
                // remove foreach input variable to avoid problems when running in variable
                // strict mode
                parameters.remove(getSubProcessNode().getMetaData("MICollectionInput"));
            }

            ProcessInstance processInstance = null;
            if (((WorkflowProcessInstanceImpl) getProcessInstance()).getCorrelationKey() != null) {
                // in case there is correlation key on parent instance pass it along to child so
                // it can be easily correlated
                // since correlation key must be unique for active instances it appends
                // processId and timestamp
                List<String> businessKeys = new ArrayList<String>();
                businessKeys.add(((WorkflowProcessInstanceImpl) getProcessInstance()).getCorrelationKey());
                businessKeys.add(processId);
                businessKeys.add(String.valueOf(System.currentTimeMillis()));

                CorrelationKey subProcessCorrelationKey = new StringCorrelationKey(
                        businessKeys.stream().collect(Collectors.joining(":")));
                processInstance = (ProcessInstance) ((CorrelationAwareProcessRuntime) kruntime)
                        .createProcessInstance(processId, subProcessCorrelationKey, parameters);
            } else {
                processInstance = (ProcessInstance) kruntime.createProcessInstance(processId, parameters);
            }
            this.processInstanceId = processInstance.getId();
            ((ProcessInstanceImpl) processInstance).setMetaData("ParentProcessInstanceId",
                    getProcessInstance().getId());
            ((ProcessInstanceImpl) processInstance).setMetaData("ParentNodeInstanceId", getUniqueId());
            ((ProcessInstanceImpl) processInstance).setMetaData("ParentNodeId", getSubProcessNode().getUniqueId());
            ((ProcessInstanceImpl) processInstance).setParentProcessInstanceId(getProcessInstance().getId());
            ((ProcessInstanceImpl) processInstance).setRootProcessInstanceId(
                    StringUtils.isEmpty(getProcessInstance().getRootProcessInstanceId()) ? getProcessInstance().getId()
                            : getProcessInstance().getRootProcessInstanceId());
            ((ProcessInstanceImpl) processInstance).setRootProcessId(
                    StringUtils.isEmpty(getProcessInstance().getRootProcessId()) ? getProcessInstance().getProcessId()
                            : getProcessInstance().getRootProcessId());
            ((ProcessInstanceImpl) processInstance).setSignalCompletion(getSubProcessNode().isWaitForCompletion());
            ((ProcessInstanceImpl) processInstance).addChild(processInstance.getProcessId(), processInstance.getId());

            kruntime.startProcessInstance(processInstance.getId());
            if (!getSubProcessNode().isWaitForCompletion()) {
                triggerCompleted();
            } else if (processInstance.getState() == ProcessInstance.STATE_COMPLETED
                    || processInstance.getState() == ProcessInstance.STATE_ABORTED) {
                processInstanceCompleted(processInstance);
            } else {
                addProcessListener();
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (getSubProcessNode() == null || !getSubProcessNode().isIndependent()) {
            ProcessInstance processInstance = null;
            InternalProcessRuntime kruntime = ((ProcessInstance) getProcessInstance()).getProcessRuntime();

            processInstance = (ProcessInstance) kruntime.getProcessInstance(processInstanceId);

            if (processInstance != null) {
                processInstance.setState(ProcessInstance.STATE_ABORTED);
            }
        }
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void internalSetProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
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
        ((ProcessInstanceImpl) getProcessInstance()).removeChild(processInstance.getProcessId(),
                processInstance.getId());
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
                ((ProcessInstance) getProcessInstance()).setState(ProcessInstance.STATE_ABORTED, faultName);
                return;
            }

        }
        // handle dynamic subprocess
        if (getNode() == null) {
            setMetaData("NodeType", "SubProcessNode");
        }
        // if there were no exception proceed normally
        triggerCompleted();

    }

    private void handleOutMappings(ProcessInstance processInstance) {
        VariableScopeInstance subProcessVariableScopeInstance = (VariableScopeInstance) processInstance
                .getContextInstance(VariableScope.VARIABLE_SCOPE);
        SubProcessNode subProcessNode = getSubProcessNode();
        if (subProcessNode != null) {
            for (Iterator<io.automatik.engine.workflow.process.core.node.DataAssociation> iterator = subProcessNode
                    .getOutAssociations().iterator(); iterator.hasNext();) {
                io.automatik.engine.workflow.process.core.node.DataAssociation mapping = iterator.next();
                if (mapping.getTransformation() != null) {
                    Transformation transformation = mapping.getTransformation();
                    DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                    if (transformer != null) {
                        Map<String, Object> dataSet = new HashMap<String, Object>();
                        if (getNodeInstanceContainer() instanceof CompositeContextNodeInstance) {
                            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) ((CompositeContextNodeInstance) getNodeInstanceContainer())
                                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
                            if (variableScopeInstance != null) {
                                dataSet.putAll(variableScopeInstance.getVariables());
                            }
                        }
                        dataSet.putAll(subProcessVariableScopeInstance.getVariables());
                        Object parameterValue = transformer.transform(transformation.getCompiledExpression(), dataSet);

                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VariableScope.VARIABLE_SCOPE, mapping.getTarget());
                        if (variableScopeInstance != null && parameterValue != null) {

                            variableScopeInstance.setVariable(this, mapping.getTarget(), parameterValue);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", mapping.getTarget());
                            logger.warn("Continuing without setting variable.");
                        }
                    }
                } else {
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VariableScope.VARIABLE_SCOPE, mapping.getTarget());
                    if (variableScopeInstance != null) {
                        Object value = subProcessVariableScopeInstance.getVariable(mapping.getSources().get(0));
                        if (value == null) {
                            try {
                                value = MVEL.eval(mapping.getSources().get(0),
                                        new VariableScopeResolverFactory(subProcessVariableScopeInstance));
                            } catch (Throwable t) {
                                // do nothing
                            }
                        }
                        variableScopeInstance.setVariable(this, mapping.getTarget(), value);
                    } else {
                        String output = mapping.getSources().get(0);
                        String target = mapping.getTarget();

                        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                        if (matcher.find()) {
                            String paramName = matcher.group(1);

                            String expression = paramName + " = " + output;
                            VariableScopeResolverFactory resolver = new VariableScopeResolverFactory(
                                    subProcessVariableScopeInstance);
                            resolver.addExtraParameters(((VariableScopeInstance) getProcessInstance()
                                    .getContextInstance(VariableScope.VARIABLE_SCOPE)).getVariables());
                            Serializable compiled = MVEL.compileExpression(expression);
                            MVEL.executeExpression(compiled, resolver);
                        } else {
                            logger.error("Could not find variable scope for variable {}", mapping.getTarget());
                            logger.error("when trying to complete SubProcess node {}", getSubProcessNode().getName());
                            logger.error("Continuing without setting variable.");
                        }
                    }
                }
            }
        } else {
            // handle dynamic sub processes without data output mapping
            mapDynamicOutputData(subProcessVariableScopeInstance.getVariables());
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
                    parameterValue = MVEL.eval(sourceParam, new NodeInstanceResolverFactory(this));
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
