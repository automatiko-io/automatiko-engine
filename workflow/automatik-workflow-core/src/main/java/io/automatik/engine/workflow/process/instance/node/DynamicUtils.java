
package io.automatik.engine.workflow.process.instance.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.services.correlation.CorrelationKey;
import io.automatik.engine.services.correlation.StringCorrelationKey;
import io.automatik.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatik.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatik.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.ProcessInstanceResolverFactory;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatik.engine.workflow.util.PatternConstants;

public class DynamicUtils {

    private static final Logger logger = LoggerFactory.getLogger(DynamicUtils.class);

    public static void addDynamicWorkItem(final DynamicNodeInstance dynamicContext, InternalProcessRuntime runtime,
            String workItemName, Map<String, Object> parameters) {
        final WorkflowProcessInstance processInstance = dynamicContext.getProcessInstance();
        internalAddDynamicWorkItem(processInstance, dynamicContext, runtime, workItemName, parameters);
    }

    public static void addDynamicWorkItem(
            final io.automatik.engine.api.runtime.process.ProcessInstance dynamicProcessInstance,
            InternalProcessRuntime runtime, String workItemName, Map<String, Object> parameters) {
        internalAddDynamicWorkItem((WorkflowProcessInstance) dynamicProcessInstance, null, runtime, workItemName,
                parameters);
    }

    private static void internalAddDynamicWorkItem(final WorkflowProcessInstance processInstance,
            final DynamicNodeInstance dynamicContext, InternalProcessRuntime runtime, String workItemName,
            Map<String, Object> parameters) {
        final WorkItemImpl workItem = new WorkItemImpl();
        workItem.setState(WorkItem.ACTIVE);
        workItem.setProcessInstanceId(processInstance.getId());
        workItem.setName(workItemName);
        workItem.setParameters(parameters);

        for (Map.Entry<String, Object> entry : workItem.getParameters().entrySet()) {
            if (entry.getValue() instanceof String) {
                String s = (String) entry.getValue();
                Object variableValue = null;
                String defaultValue = null;
                Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
                while (matcher.find()) {
                    String paramName = matcher.group(1);
                    if (paramName.contains(":")) {

                        String[] items = paramName.split(":");
                        paramName = items[0];
                        defaultValue = items[1];
                    }
                    variableValue = processInstance.getVariable(paramName);
                    if (variableValue == null) {
                        try {
                            variableValue = MVEL.eval(paramName, new ProcessInstanceResolverFactory(processInstance));
                        } catch (Throwable t) {
                            logger.error("Could not find variable scope for variable {}", paramName);
                            logger.error("when trying to replace variable in string for Dynamic Work Item {}",
                                    workItemName);
                            logger.error("Continuing without setting parameter.");
                        }
                    }
                }
                if (variableValue != null) {
                    workItem.setParameter(entry.getKey(), variableValue == null ? defaultValue : variableValue);
                }
            }
        }

        final WorkItemNodeInstance workItemNodeInstance = new WorkItemNodeInstance();
        workItemNodeInstance.internalSetWorkItem(workItem);
        workItemNodeInstance.setMetaData("NodeType", workItemName);
        workItem.setNodeInstanceId(workItemNodeInstance.getId());

        workItemNodeInstance.setProcessInstance(processInstance);
        workItemNodeInstance.setNodeInstanceContainer(dynamicContext == null ? processInstance : dynamicContext);
        workItemNodeInstance.addEventListeners();
        executeWorkItem(runtime, workItem, workItemNodeInstance);

    }

    private static void executeWorkItem(InternalProcessRuntime runtime, WorkItemImpl workItem,
            WorkItemNodeInstance workItemNodeInstance) {
        ProcessEventSupport eventSupport = runtime.getProcessEventSupport();
        eventSupport.fireBeforeNodeTriggered(workItemNodeInstance, runtime);
        ((DefaultWorkItemManager) runtime.getWorkItemManager()).internalExecuteWorkItem(workItem);
        workItemNodeInstance.internalSetWorkItemId(workItem.getId());
        eventSupport.fireAfterNodeTriggered(workItemNodeInstance, runtime);
    }

    private static DynamicNodeInstance findDynamicContext(WorkflowProcessInstance processInstance, String uniqueId) {
        for (NodeInstance nodeInstance : ((WorkflowProcessInstanceImpl) processInstance).getNodeInstances(true)) {
            if (uniqueId.equals(((NodeInstanceImpl) nodeInstance).getUniqueId())) {
                return (DynamicNodeInstance) nodeInstance;
            }
        }
        throw new IllegalArgumentException("Could not find node instance " + uniqueId);
    }

    public static String addDynamicSubProcess(final DynamicNodeInstance dynamicContext, InternalProcessRuntime runtime,
            final String processId, final Map<String, Object> parameters) {
        final WorkflowProcessInstance processInstance = dynamicContext.getProcessInstance();
        return internalAddDynamicSubProcess(processInstance, dynamicContext, runtime, processId, parameters);
    }

    public static String addDynamicSubProcess(
            final io.automatik.engine.api.runtime.process.ProcessInstance processInstance,
            InternalProcessRuntime runtime, final String processId, final Map<String, Object> parameters) {
        return internalAddDynamicSubProcess((WorkflowProcessInstance) processInstance, null, runtime, processId,
                parameters);
    }

    public static String internalAddDynamicSubProcess(final WorkflowProcessInstance processInstance,
            final DynamicNodeInstance dynamicContext, InternalProcessRuntime runtime, final String processId,
            final Map<String, Object> parameters) {
        final SubProcessNodeInstance subProcessNodeInstance = new SubProcessNodeInstance();
        subProcessNodeInstance.setNodeInstanceContainer(dynamicContext == null ? processInstance : dynamicContext);
        subProcessNodeInstance.setProcessInstance(processInstance);
        subProcessNodeInstance.setMetaData("NodeType", "SubProcessNode");
        return executeSubProcess(runtime, processId, parameters, processInstance, subProcessNodeInstance);

    }

    private static String executeSubProcess(InternalProcessRuntime runtime, String processId,
            Map<String, Object> parameters, ProcessInstance processInstance,
            SubProcessNodeInstance subProcessNodeInstance) {
        Process process = runtime.getProcess(processId);
        if (process == null) {
            logger.error("Could not find process {}", processId);
            throw new IllegalArgumentException("No process definition found with id: " + processId);
        } else {
            ProcessEventSupport eventSupport = runtime.getProcessEventSupport();
            eventSupport.fireBeforeNodeTriggered(subProcessNodeInstance, runtime);

            ProcessInstance subProcessInstance = null;
            if (((WorkflowProcessInstanceImpl) processInstance).getCorrelationKey() != null) {
                List<String> businessKeys = new ArrayList<>();
                businessKeys.add(((WorkflowProcessInstanceImpl) processInstance).getCorrelationKey());
                businessKeys.add(processId);
                businessKeys.add(String.valueOf(System.currentTimeMillis()));
                CorrelationKey subProcessCorrelationKey = new StringCorrelationKey(
                        businessKeys.stream().collect(Collectors.joining(":")));
                subProcessInstance = (ProcessInstance) runtime.createProcessInstance(processId,
                        subProcessCorrelationKey, parameters);
            } else {
                subProcessInstance = (ProcessInstance) runtime.createProcessInstance(processId, parameters);
            }

            ((ProcessInstanceImpl) subProcessInstance).setMetaData("ParentProcessInstanceId", processInstance.getId());
            ((ProcessInstanceImpl) subProcessInstance).setParentProcessInstanceId(processInstance.getId());

            subProcessInstance = (ProcessInstance) runtime.startProcessInstance(subProcessInstance.getId());
            subProcessNodeInstance.internalSetProcessInstanceId(subProcessInstance.getId());

            eventSupport.fireAfterNodeTriggered(subProcessNodeInstance, runtime);
            if (subProcessInstance
                    .getState() == io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED) {
                subProcessNodeInstance.triggerCompleted();
            } else {

                subProcessNodeInstance.addEventListeners();
            }

            return subProcessInstance.getId();
        }
    }

    private DynamicUtils() {
        // It is not allowed to create instances of util classes.
    }
}
