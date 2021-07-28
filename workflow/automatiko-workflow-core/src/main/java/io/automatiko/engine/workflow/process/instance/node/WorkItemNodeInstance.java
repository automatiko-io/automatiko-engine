
package io.automatiko.engine.workflow.process.instance.node;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ABORTED;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatiko.engine.api.runtime.process.WorkItem.ABORTED;
import static io.automatiko.engine.api.runtime.process.WorkItem.COMPLETED;
import static io.automatiko.engine.workflow.base.core.context.variable.VariableScope.VARIABLE_SCOPE;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessWorkItemHandlerException;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.api.workflow.GroupedNamedDataType;
import io.automatiko.engine.api.workflow.IOEventDescription;
import io.automatiko.engine.api.workflow.NamedDataType;
import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.workflow.base.core.Context;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.exception.ExceptionScopeInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactory;
import io.automatiko.engine.workflow.base.instance.impl.ContextInstanceFactoryRegistry;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemHandlerNotFoundException;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.Transformation;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowRuntimeException;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatiko.engine.workflow.process.instance.impl.WorkItemResolverFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of a work item node.
 * 
 */
public class WorkItemNodeInstance extends StateBasedNodeInstance implements EventListener, ContextInstanceContainer {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(WorkItemNodeInstance.class);

    private static List<String> defaultOutputVariables = Arrays.asList("ActorId");

    private Map<String, List<ContextInstance>> subContextInstances = new HashMap<>();

    private String workItemId;
    private transient WorkItem workItem;

    private String exceptionHandlingProcessInstanceId;

    protected WorkItemNode getWorkItemNode() {
        return (WorkItemNode) getNode();
    }

    public WorkItem getWorkItem() {
        if (workItem == null && workItemId != null) {
            workItem = ((DefaultWorkItemManager) getProcessInstance().getProcessRuntime().getWorkItemManager())
                    .getWorkItem(workItemId);
        }
        return workItem;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public void internalSetWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }

    public void internalSetWorkItem(WorkItem workItem) {
        this.workItem = workItem;
        this.workItem.setProcessInstance(getProcessInstance());
        this.workItem.setNodeInstance(this);
    }

    @Override
    public boolean isInversionOfControl() {
        // TODO WorkItemNodeInstance.isInversionOfControl
        return false;
    }

    public void internalRegisterWorkItem() {
        ((DefaultWorkItemManager) getProcessInstance().getProcessRuntime().getWorkItemManager())
                .internalAddWorkItem(workItem);
    }

    @Override
    public void internalTrigger(final NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }

        WorkItemNode workItemNode = getWorkItemNode();
        createWorkItem(workItemNode);
        if (workItemNode.isWaitForCompletion()) {
            addWorkItemListener();
        }
        ((WorkItemImpl) workItem).setNodeInstanceId(this.getId());
        ((WorkItemImpl) workItem).setNodeId(getNodeId());
        workItem.setNodeInstance(this);
        workItem.setProcessInstance(getProcessInstance());

        try {
            ((DefaultWorkItemManager) getProcessInstance().getProcessRuntime().getWorkItemManager())
                    .internalExecuteWorkItem(workItem);
        } catch (WorkItemHandlerNotFoundException wihnfe) {
            getProcessInstance().setState(STATE_ABORTED);
            throw wihnfe;
        } catch (ProcessWorkItemHandlerException handlerException) {
            this.workItemId = workItem.getId();
            removeEventListeners();
            handleWorkItemHandlerException(handlerException, workItem);
        } catch (WorkItemExecutionError e) {
            removeEventListeners();
            handleException(e.getErrorCode(), e);
        } catch (Exception e) {
            removeEventListeners();
            String exceptionName = e.getClass().getName();
            handleException(exceptionName, e);
        }

        if (!workItemNode.isWaitForCompletion()) {
            triggerCompleted();
        }
        this.workItemId = workItem.getId();
    }

    protected void handleException(String exceptionName, Exception e) {
        ExceptionScopeInstance exceptionScopeInstance = (ExceptionScopeInstance) resolveContextInstance(
                ExceptionScope.EXCEPTION_SCOPE, exceptionName);
        if (exceptionScopeInstance == null) {
            if (e instanceof WorkItemExecutionError) {
                throw (WorkItemExecutionError) e;
            }

            throw new WorkflowRuntimeException(this, getProcessInstance(),
                    "Unable to execute Action: " + e.getMessage(), e);
        }
        // workItemId must be set otherwise cancel activity will not find the right work
        // item
        this.workItemId = workItem.getId();
        Object param = e;
        if (e instanceof WorkItemExecutionError) {
            param = ((WorkItemExecutionError) e).getErrorData();
        }
        exceptionScopeInstance.handleException(this, exceptionName, param != null ? param : e);

    }

    protected WorkItem newWorkItem() {
        return new WorkItemImpl();
    }

    protected WorkItem createWorkItem(WorkItemNode workItemNode) {
        Work work = workItemNode.getWork();
        if (workItem == null) {
            workItem = newWorkItem();
            ((WorkItemImpl) workItem).setName(work.getName());
            ((WorkItemImpl) workItem).setProcessInstanceId(getProcessInstance().getId());
            ((WorkItemImpl) workItem).setParameters(new HashMap<>(work.getParameters()));
            workItem.setStartDate(new Date());
        }
        // if there are any dynamic parameters add them
        if (dynamicParameters != null) {
            workItem.getParameters().putAll(dynamicParameters);
        }

        for (DataAssociation association : workItemNode.getInAssociations()) {
            if (association.getTransformation() != null) {
                Transformation transformation = association.getTransformation();
                DataTransformer transformer = DataTransformerRegistry.get().find(transformation.getLanguage());
                if (transformer != null) {
                    Object parameterValue = transformer.transform(transformation.getCompiledExpression(),
                            getSourceParameters(association));
                    if (parameterValue != null) {
                        ((WorkItemImpl) workItem).setParameter(association.getTarget(), parameterValue);
                    }
                }
            } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                Object parameterValue = null;
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VARIABLE_SCOPE, association.getSources().get(0));
                if (variableScopeInstance != null) {
                    parameterValue = variableScopeInstance.getVariable(association.getSources().get(0));
                } else {
                    try {
                        parameterValue = MVEL.eval(association.getSources().get(0),
                                new NodeInstanceResolverFactory(this));
                    } catch (Throwable t) {
                        logger.error("Could not find variable scope for variable {}", association.getSources().get(0));
                        logger.error("when trying to execute Work Item {}", work.getName());
                        logger.error("Continuing without setting parameter.");
                    }
                }
                if (parameterValue != null) {
                    ((WorkItemImpl) workItem).setParameter(association.getTarget(), parameterValue);
                }
            } else {
                association.getAssignments().stream().forEach(this::handleAssignment);
            }
        }

        for (Map.Entry<String, Object> entry : workItem.getParameters().entrySet()) {
            if (entry.getValue() instanceof String) {
                String s = (String) entry.getValue();
                Map<String, String> replacements = new HashMap<>();
                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
                while (matcher.find()) {
                    String paramName = matcher.group(1);
                    String replacementKey = paramName;
                    String defaultValue = "";
                    if (paramName.contains(":")) {

                        String[] items = paramName.split(":");
                        paramName = items[0];
                        defaultValue = items[1];
                    }
                    if (replacements.get(replacementKey) == null) {
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VARIABLE_SCOPE, paramName);
                        if (variableScopeInstance != null) {
                            Object variableValue = variableScopeInstance.getVariable(paramName);
                            String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                            replacements.put(replacementKey, variableValueString);
                        } else {
                            try {
                                Object variableValue = MVEL.eval(paramName, new NodeInstanceResolverFactory(this));
                                String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                                replacements.put(replacementKey, variableValueString);
                            } catch (Throwable t) {
                                logger.error("Could not find variable scope for variable {}", paramName);
                                logger.error("when trying to replace variable in string for Work Item {}",
                                        work.getName());
                                logger.error("Continuing without setting parameter.");
                            }
                        }
                    }
                }

                for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                    s = s.replace("#{" + replacement.getKey() + "}", replacement.getValue());
                }
                ((WorkItemImpl) workItem).setParameter(entry.getKey(), s);

            }
        }
        return workItem;
    }

    private void handleAssignment(Assignment assignment) {
        AssignmentAction action = (AssignmentAction) assignment.getMetaData("Action");
        try {
            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
            context.setNodeInstance(this);
            action.execute(getWorkItem(), context);
        } catch (WorkItemExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("unable to execute Assignment", e);
        }
    }

    public void triggerCompleted(WorkItem workItem) {
        this.workItem = workItem;
        WorkItemNode workItemNode = getWorkItemNode();

        if (workItemNode != null && workItem.getState() == WorkItem.ABORTED) {
            cancel();
            continueToNextNode(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, workItemNode);
            return;
        } else if (workItemNode != null && workItem.getState() == WorkItem.COMPLETED) {

            validateWorkItemResultVariable(getProcessInstance().getProcessName(), workItemNode.getOutAssociations(),
                    workItem);
            for (Iterator<DataAssociation> iterator = getWorkItemNode().getOutAssociations().iterator(); iterator
                    .hasNext();) {
                DataAssociation association = iterator.next();
                if (association.getTransformation() != null) {
                    Transformation transformation = association.getTransformation();
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
                        dataSet.putAll(workItem.getParameters());
                        dataSet.putAll(workItem.getResults());

                        Object parameterValue = transformer.transform(transformation.getCompiledExpression(), dataSet);

                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VARIABLE_SCOPE, association.getTarget());
                        if (variableScopeInstance != null && parameterValue != null) {

                            variableScopeInstance.getVariableScope().validateVariable(
                                    getProcessInstance().getProcessName(), association.getTarget(), parameterValue);

                            variableScopeInstance.setVariable(this, association.getTarget(), parameterValue);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                            logger.warn("when trying to complete Work Item {}", workItem.getName());
                            logger.warn("Continuing without setting variable.");
                        }
                        if (parameterValue != null) {
                            ((WorkItemImpl) workItem).setParameter(association.getTarget(), parameterValue);
                        }
                    }
                } else if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                    VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                            VARIABLE_SCOPE, association.getTarget());
                    if (variableScopeInstance != null) {
                        Object value = workItem.getResult(association.getSources().get(0));
                        if (value == null) {
                            try {
                                value = MVEL.eval(association.getSources().get(0),
                                        new WorkItemResolverFactory(workItem));
                            } catch (Throwable t) {
                                // do nothing
                            }
                        }
                        Variable varDef = variableScopeInstance.getVariableScope()
                                .findVariable(association.getTarget());
                        DataType dataType = varDef.getType();
                        // exclude java.lang.Object as it is considered unknown type
                        if (!dataType.getStringType().endsWith("java.lang.Object")
                                && !dataType.getStringType().endsWith("Object") && value instanceof String) {
                            value = dataType.readValue((String) value);
                        } else {
                            variableScopeInstance.getVariableScope().validateVariable(
                                    getProcessInstance().getProcessName(), association.getTarget(), value);
                        }
                        variableScopeInstance.setVariable(this, association.getTarget(), value);
                    } else {
                        String output = association.getSources().get(0);
                        String target = association.getTarget();

                        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                        if (matcher.find()) {
                            String paramName = matcher.group(1);

                            String expression = VariableUtil.transformDotNotation(paramName, output);
                            NodeInstanceResolverFactory resolver = new NodeInstanceResolverFactory(this);
                            resolver.addExtraParameters(workItem.getResults());
                            Serializable compiled = MVEL.compileExpression(expression);
                            MVEL.executeExpression(compiled, resolver);
                        } else {
                            logger.warn("Could not find variable scope for variable {}", association.getTarget());
                            logger.warn("when trying to complete Work Item {}", workItem.getName());
                            logger.warn("Continuing without setting variable.");
                        }
                    }

                } else {
                    try {
                        association.getAssignments().forEach(this::handleAssignment);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        // handle dynamic nodes
        if (getNode() == null) {
            setMetaData("NodeType", workItem.getName());
            mapDynamicOutputData(workItem.getResults());
        }
        triggerCompleted();
    }

    @Override
    public void cancel() {
        WorkItem item = getWorkItem();
        if (item != null && item.getState() != COMPLETED && item.getState() != ABORTED) {
            try {
                ((DefaultWorkItemManager) getProcessInstance().getProcessRuntime().getWorkItemManager())
                        .internalAbortWorkItem(item.getId());
            } catch (WorkItemHandlerNotFoundException wihnfe) {
                getProcessInstance().setState(STATE_ABORTED);
                throw wihnfe;
            }
        }

        if (exceptionHandlingProcessInstanceId != null) {
            ProcessInstance processInstance = (ProcessInstance) getProcessInstance().getProcessRuntime()
                    .getProcessInstance(exceptionHandlingProcessInstanceId);
            if (processInstance != null) {
                processInstance.setState(STATE_ABORTED);
            }
        }
        super.cancel();
    }

    @Override
    public void addEventListeners() {
        super.addEventListeners();
        addWorkItemListener();
        addExceptionProcessListener();
    }

    private void addWorkItemListener() {
        getProcessInstance().addEventListener("workItemCompleted", this, false);
        getProcessInstance().addEventListener("workItemAborted", this, false);
    }

    @Override
    public void removeEventListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener("workItemCompleted", this, false);
        getProcessInstance().removeEventListener("workItemAborted", this, false);
    }

    @Override
    public void signalEvent(String type, Object event) {
        if ("workItemCompleted".equals(type)) {
            workItemCompleted((WorkItem) event);
        } else if ("workItemAborted".equals(type)) {
            workItemAborted((WorkItem) event);
        } else if (("processInstanceCompleted:" + exceptionHandlingProcessInstanceId).equals(type)) {
            exceptionHandlingCompleted((ProcessInstance) event, null);
        } else if (type.equals("RuleFlow-Activate" + getProcessInstance().getProcessId() + "-"
                + getNode().getMetaData().get("UniqueId"))) {

            trigger(null, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        } else {
            super.signalEvent(type, event);
        }
    }

    public String[] getEventTypes() {
        if (exceptionHandlingProcessInstanceId != null) {
            return new String[] { "workItemCompleted",
                    "processInstanceCompleted:" + exceptionHandlingProcessInstanceId };
        } else {
            return new String[] { "workItemCompleted" };
        }
    }

    public void workItemAborted(WorkItem workItem) {
        if (workItem.getId().equals(workItemId)
                || (workItemId == null && getWorkItem().getId().equals(workItem.getId()))) {
            removeEventListeners();
            triggerCompleted(workItem);
        }
    }

    public void workItemCompleted(WorkItem workItem) {
        if (workItem.getId().equals(workItemId)
                || (workItemId == null && getWorkItem().getId().equals(workItem.getId()))) {
            removeEventListeners();
            triggerCompleted(workItem);
        }
    }

    @Override
    public String getNodeName() {
        Node node = getNode();
        if (node == null) {
            String nodeName = "[Dynamic]";
            WorkItem item = getWorkItem();
            if (item != null) {
                nodeName += " " + item.getParameter("TaskName");
            }
            return nodeName;
        }
        return super.getNodeName();
    }

    @Override
    public List<ContextInstance> getContextInstances(String contextId) {
        return this.subContextInstances.get(contextId);
    }

    @Override
    public void addContextInstance(String contextId, ContextInstance contextInstance) {
        this.subContextInstances.computeIfAbsent(contextId, k -> new ArrayList<>()).add(contextInstance);
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
        List<ContextInstance> instances = subContextInstances.get(contextId);
        if (instances != null) {
            for (ContextInstance contextInstance : instances) {
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
        ContextInstance contextInstance = conf.getContextInstance(context, this, getProcessInstance());
        if (contextInstance == null) {
            throw new IllegalArgumentException("Illegal context type (instance not found): " + context.getClass());
        }
        return contextInstance;
    }

    @Override
    public ContextContainer getContextContainer() {
        return getWorkItemNode();
    }

    protected Map<String, Object> getSourceParameters(DataAssociation association) {
        Map<String, Object> parameters = new HashMap<>();
        for (String sourceParam : association.getSources()) {
            Object parameterValue = null;
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(VARIABLE_SCOPE,
                    sourceParam);
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

    public void validateWorkItemResultVariable(String processName, List<DataAssociation> outputs, WorkItem workItem) {
        // in case work item results are skip validation as there is no notion of
        // mandatory data outputs
        if (!VariableScope.isVariableStrictEnabled() || workItem.getResults().isEmpty()) {
            return;
        }

        List<String> outputNames = new ArrayList<>();
        for (DataAssociation association : outputs) {
            if (association.getSources() != null) {
                outputNames.add(association.getSources().get(0));
            }
            if (association.getAssignments() != null) {
                association.getAssignments().forEach(a -> outputNames.add(a.getFrom()));
            }
        }

        for (String outputName : workItem.getResults().keySet()) {
            if (!outputNames.contains(outputName) && !defaultOutputVariables.contains(outputName)) {
                throw new IllegalArgumentException("Data output '" + outputName + "' is not defined in process '"
                        + processName + "' for task '" + workItem.getParameter("NodeName") + "'");
            }
        }
    }

    /*
     * Work item handler exception handling
     */

    private void handleWorkItemHandlerException(ProcessWorkItemHandlerException handlerException, WorkItem workItem) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("ProcessInstanceId", workItem.getProcessInstanceId());
        parameters.put("WorkItemId", workItem.getId());
        parameters.put("NodeInstanceId", this.getId());
        parameters.put("ErrorMessage", handlerException.getMessage());
        parameters.put("Error", handlerException);

        // add all parameters of the work item to the newly started process instance
        parameters.putAll(workItem.getParameters());

        ProcessInstance processInstance = (ProcessInstance) getProcessInstance().getProcessRuntime()
                .createProcessInstance(handlerException.getProcessId(), parameters);

        this.exceptionHandlingProcessInstanceId = processInstance.getId();
        ((ProcessInstanceImpl) processInstance).setMetaData("ParentProcessInstanceId", getProcessInstance().getId());
        ((ProcessInstanceImpl) processInstance).setMetaData("ParentNodeInstanceId", getUniqueId());

        processInstance.setParentProcessInstanceId(getProcessInstance().getId());
        processInstance.setSignalCompletion(true);

        getProcessInstance().getProcessRuntime().startProcessInstance(processInstance.getId());
        if (processInstance.getState() == STATE_COMPLETED || processInstance.getState() == STATE_ABORTED) {
            exceptionHandlingCompleted(processInstance, handlerException);
        } else {
            addExceptionProcessListener();
        }
    }

    private void exceptionHandlingCompleted(ProcessInstance processInstance,
            ProcessWorkItemHandlerException handlerException) {

        if (handlerException == null) {
            handlerException = (ProcessWorkItemHandlerException) ((WorkflowProcessInstance) processInstance)
                    .getVariable("Error");
        }

        switch (handlerException.getStrategy()) {
            case ABORT:
                getProcessInstance().getProcessRuntime().getWorkItemManager().abortWorkItem(getWorkItem().getId());
                break;
            case RETHROW:
                String exceptionName = handlerException.getCause().getClass().getName();
                ExceptionScopeInstance exceptionScopeInstance = (ExceptionScopeInstance) resolveContextInstance(
                        ExceptionScope.EXCEPTION_SCOPE, exceptionName);
                if (exceptionScopeInstance == null) {
                    throw new WorkflowRuntimeException(this, getProcessInstance(),
                            "Unable to execute work item " + handlerException.getMessage(), handlerException.getCause());
                }

                exceptionScopeInstance.handleException(this, exceptionName, handlerException.getCause());
                break;
            case RETRY:
                Map<String, Object> parameters = new HashMap<>(getWorkItem().getParameters());

                parameters.putAll(processInstance.getVariables());

                ((DefaultWorkItemManager) getProcessInstance().getProcessRuntime().getWorkItemManager())
                        .retryWorkItem(getWorkItem().getId(), parameters);
                break;
            case COMPLETE:
                getProcessInstance().getProcessRuntime().getWorkItemManager().completeWorkItem(getWorkItem().getId(),
                        processInstance.getVariables());
                break;
            default:
                break;
        }

    }

    public void addExceptionProcessListener() {
        if (exceptionHandlingProcessInstanceId != null) {
            getProcessInstance().addEventListener("processInstanceCompleted:" + exceptionHandlingProcessInstanceId,
                    this, true);
        }
    }

    public void removeExceptionProcessListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener("processInstanceCompleted:" + exceptionHandlingProcessInstanceId, this,
                true);
    }

    public String getExceptionHandlingProcessInstanceId() {
        return exceptionHandlingProcessInstanceId;
    }

    public void internalSetProcessInstanceId(String processInstanceId) {
        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            this.exceptionHandlingProcessInstanceId = processInstanceId;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<EventDescription<?>> getEventDescriptions() {
        List<NamedDataType> inputs = new ArrayList<>();
        for (ParameterDefinition paramDef : getWorkItemNode().getWork().getParameterDefinitions()) {
            inputs.add(new NamedDataType(paramDef.getName(), paramDef.getType()));
        }

        List<NamedDataType> outputs = new ArrayList<>();
        Map<String, Object> dataOutputs = (Map<String, Object>) getWorkItemNode().getMetaData().getOrDefault("DataOutputs",
                Collections.emptyMap());

        for (Entry<String, Object> dOut : dataOutputs.entrySet()) {
            outputs.add(new NamedDataType(dOut.getKey(), dOut.getValue()));
        }

        GroupedNamedDataType dataTypes = new GroupedNamedDataType();
        dataTypes.add("Input", inputs);
        dataTypes.add("Output", outputs);

        Map<String, String> properties = new HashMap<>();

        if (getWorkItem() instanceof HumanTaskWorkItem) {
            properties.put("ActualOwner", ((HumanTaskWorkItem) getWorkItem()).getActualOwner());
            properties.put("PotentialUsers",
                    ((HumanTaskWorkItem) getWorkItem()).getPotentialUsers().stream().collect(Collectors.joining(",")));
            properties.put("PotentialGroups",
                    ((HumanTaskWorkItem) getWorkItem()).getPotentialGroups().stream().collect(Collectors.joining(",")));
        }

        // return just the main completion type of an event
        return Collections.singleton(new IOEventDescription("workItemCompleted", getNodeDefinitionId(), getNodeName(),
                "workItem", getWorkItemId(), getProcessInstance().getId(), dataTypes, properties));
    }

    public String buildReferenceId() {
        StringBuilder builder = new StringBuilder();
        builder.append("/" + getProcessInstance().getReferenceFromRoot())
                .append(sanitizeName((String) getWorkItem().getParameters().getOrDefault("TaskName", getNodeName())))
                .append("/")
                .append(getWorkItem().getId());

        return builder.toString();
    }

    @Override
    public void retry() {

        super.retry();
    }

    @Override
    protected String captureError(Exception e) {
        removeEventListeners();
        return super.captureError(e);
    }

    private String sanitizeName(String name) {
        return name.replaceAll("\\s", "_");
    }

    public String buildFormLink() {
        if (getProcessInstance().getProcess().getMetaData().containsKey("UserTaskMgmt")) {
            String parentProcessInstanceId = getProcessInstance().getParentProcessInstanceId();
            if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
                parentProcessInstanceId += ":";
            } else {
                parentProcessInstanceId = "";
            }
            String version = version(getProcessInstance().getProcess().getVersion());
            String encoded = Base64.getEncoder().encodeToString((getProcessInstance().getProcessId() + version + "|"
                    + parentProcessInstanceId + getProcessInstance().getId() + "|" + getWorkItemId() + "|")
                            .getBytes(StandardCharsets.UTF_8));
            return "/management/tasks/link/" + encoded;
        } else {
            return null;
        }
    }

    protected String version(String version) {
        if (version != null && !version.trim().isEmpty()) {
            return "_" + version.replaceAll("\\.", "_");
        }
        return "";
    }
}
