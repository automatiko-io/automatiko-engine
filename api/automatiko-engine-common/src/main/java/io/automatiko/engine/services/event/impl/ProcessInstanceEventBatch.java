
package io.automatiko.engine.services.event.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.automatiko.engine.api.Addons;
import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventBatch;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessEvent;
import io.automatiko.engine.api.event.process.ProcessNodeEvent;
import io.automatiko.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.ExecutionsErrorInfo;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;
import io.automatiko.engine.services.event.UserTaskInstanceDataEvent;
import io.automatiko.engine.services.event.VariableInstanceDataEvent;

public class ProcessInstanceEventBatch implements EventBatch {

    public static final String TRACKED = "tracked";

    private final String service;
    private Addons addons;
    private List<ProcessEvent> rawEvents = new ArrayList<>();

    public ProcessInstanceEventBatch(String service, Addons addons) {
        this.service = service;
        this.addons = addons;
    }

    @Override
    public void append(Object rawEvent) {
        if (rawEvent instanceof ProcessEvent) {
            rawEvents.add((ProcessEvent) rawEvent);
        } else if (rawEvent instanceof WorkUnit<?>) {
            append(((WorkUnit<?>) rawEvent).data());
        } else if (rawEvent instanceof Collection) {
            for (Object event : ((Collection<?>) rawEvent)) {
                append(event);
            }
        }
    }

    @Override
    public Collection<DataEvent<?>> events() {
        Map<String, ProcessInstanceEventBody> processInstances = new LinkedHashMap<>();
        Map<String, UserTaskInstanceEventBody> userTaskInstances = new LinkedHashMap<>();
        Set<VariableInstanceEventBody> variables = new LinkedHashSet<>();

        for (ProcessEvent event : rawEvents) {
            ProcessInstanceEventBody body = processInstances.computeIfAbsent(event.getProcessInstance().getId(),
                    key -> create(event));

            if (event instanceof ProcessNodeTriggeredEvent) {
                handleProcessNodeTriggeredEvent((ProcessNodeTriggeredEvent) event, body);
            } else if (event instanceof ProcessNodeLeftEvent) {
                handleProcessNodeLeftEvent((ProcessNodeLeftEvent) event, body);
            } else if (event instanceof ProcessCompletedEvent) {
                handleProcessCompletedEvent((ProcessCompletedEvent) event, body);
            } else if (event instanceof ProcessWorkItemTransitionEvent) {
                handleProcessWorkItemTransitionEvent((ProcessWorkItemTransitionEvent) event, userTaskInstances);
            } else if (event instanceof ProcessVariableChangedEvent) {
                handleProcessVariableChangedEvent((ProcessVariableChangedEvent) event, variables);
            }
        }

        Collection<DataEvent<?>> processedEvents = new ArrayList<>();

        processInstances.values().stream().map(pi -> new ProcessInstanceDataEvent(extractRuntimeSource(pi.metaData()),
                addons.toString(), pi.metaData(), pi)).forEach(processedEvents::add);
        userTaskInstances.values().stream().map(pi -> new UserTaskInstanceDataEvent(extractRuntimeSource(pi.metaData()),
                addons.toString(), pi.metaData(), pi)).forEach(processedEvents::add);
        variables.stream().map(pi -> new VariableInstanceDataEvent(extractRuntimeSource(pi.metaData()),
                addons.toString(), pi.metaData(), pi)).forEach(processedEvents::add);

        return processedEvents;
    }

    protected void handleProcessCompletedEvent(ProcessCompletedEvent event, ProcessInstanceEventBody body) {
        // in case this is a process complete event always updated and date and state
        body.update().endDate(((WorkflowProcessInstance) event.getProcessInstance()).getEndDate())
                .state(event.getProcessInstance().getState());
    }

    protected void handleProcessNodeTriggeredEvent(ProcessNodeTriggeredEvent event, ProcessInstanceEventBody body) {
        NodeInstanceEventBody nodeInstanceBody = create((ProcessNodeEvent) event);
        if (!body.getNodeInstances().contains(nodeInstanceBody)) {
            // add it only if it does not exist
            body.update().nodeInstance(nodeInstanceBody);
        }
    }

    protected void handleProcessNodeLeftEvent(ProcessNodeLeftEvent event, ProcessInstanceEventBody body) {
        NodeInstanceEventBody nodeInstanceBody = create((ProcessNodeEvent) event);
        // if it's already there, remove it
        body.getNodeInstances().remove(nodeInstanceBody);
        // and add it back as the node left event has latest information
        body.update().nodeInstance(nodeInstanceBody);
    }

    protected void handleProcessWorkItemTransitionEvent(ProcessWorkItemTransitionEvent workItemTransitionEvent,
            Map<String, UserTaskInstanceEventBody> userTaskInstances) {
        WorkItem workItem = workItemTransitionEvent.getWorkItem();
        if (workItem instanceof HumanTaskWorkItem && workItemTransitionEvent.isTransitioned()) {
            userTaskInstances.putIfAbsent(workItem.getId(), createUserTask(workItemTransitionEvent));
        }
    }

    protected void handleProcessVariableChangedEvent(ProcessVariableChangedEvent variableChangedEvent,
            Set<VariableInstanceEventBody> variables) {
        if (variableChangedEvent.hasTag(TRACKED)) {
            variables.add(create(variableChangedEvent));
        }
    }

    protected UserTaskInstanceEventBody createUserTask(ProcessWorkItemTransitionEvent workItemTransitionEvent) {
        WorkflowProcessInstance pi = (WorkflowProcessInstance) workItemTransitionEvent.getProcessInstance();
        HumanTaskWorkItem workItem = (HumanTaskWorkItem) workItemTransitionEvent.getWorkItem();
        return UserTaskInstanceEventBody.create().id(workItem.getId()).state(workItem.getPhaseStatus())
                .taskName(workItem.getTaskName()).taskDescription(workItem.getTaskDescription())
                .taskPriority(workItem.getTaskPriority()).referenceName(workItem.getReferenceName())
                .actualOwner(workItem.getActualOwner()).startDate(workItem.getStartDate())
                .completeDate(workItem.getCompleteDate()).adminGroups(workItem.getAdminGroups())
                .adminUsers(workItem.getAdminUsers()).excludedUsers(workItem.getExcludedUsers())
                .potentialGroups(workItem.getPotentialGroups()).potentialUsers(workItem.getPotentialUsers())
                .processInstanceId(pi.getId()).rootProcessInstanceId(pi.getRootProcessInstanceId())
                .processId(pi.getProcessId()).rootProcessId(pi.getRootProcessId()).inputs(workItem.getParameters())
                .outputs(workItem.getResults())
                .instance(workItem).build();
    }

    protected ProcessInstanceEventBody create(ProcessEvent event) {
        WorkflowProcessInstance pi = (WorkflowProcessInstance) event.getProcessInstance();

        ProcessInstanceEventBody.Builder eventBuilder = ProcessInstanceEventBody.create().id(pi.getId())
                .parentInstanceId(pi.getParentProcessInstanceId()).rootInstanceId(pi.getRootProcessInstanceId())
                .processId(pi.getProcessId()).rootProcessId(pi.getRootProcessId()).processName(pi.getProcessName())
                .startDate(pi.getStartDate()).endDate(pi.getEndDate()).state(pi.getState())
                .businessKey(pi.getCorrelationKey()).variables(pi.getVariables()).milestones(createMilestones(pi));

        if (pi.getState() == ProcessInstance.STATE_ERROR) {
            for (ExecutionsErrorInfo error : pi.errors()) {
                eventBuilder.error(ProcessErrorEventBody.create().nodeDefinitionId(error.getFailedNodeId())
                        .errorMessage(error.getErrorMessage()).build());
            }
        }

        String securityRoles = (String) pi.getProcess().getMetaData().get("securityRoles");
        if (securityRoles != null) {
            eventBuilder.roles(securityRoles.split(","));
        }

        Collection<Tag> tags = pi.getTags();
        if (tags != null) {
            eventBuilder.tags(tags.stream().map(t -> t.getValue()).toArray(String[]::new));
        }
        io.automatiko.engine.api.workflow.ProcessInstance<?> instance = (io.automatiko.engine.api.workflow.ProcessInstance<?>) pi
                .getMetaData("AutomatikProcessInstance");
        if (instance != null) {
            Set<String> visibleTo = instance.process().accessPolicy().visibleTo(instance);
            if (visibleTo != null) {
                eventBuilder.visibleTo(visibleTo.toArray(String[]::new));
            }
            eventBuilder.instance(instance);
        }

        return eventBuilder.build();
    }

    protected Set<MilestoneEventBody> createMilestones(WorkflowProcessInstance pi) {
        if (pi.milestones() == null) {
            return null;
        }

        return pi.milestones().stream().map(
                m -> MilestoneEventBody.create().id(m.getId()).name(m.getName()).status(m.getStatus().name()).build())
                .collect(Collectors.toSet());
    }

    protected NodeInstanceEventBody create(ProcessNodeEvent event) {
        NodeInstance ni = event.getNodeInstance();

        return NodeInstanceEventBody.create().id(ni.getId()).nodeId(String.valueOf(ni.getNodeId()))
                .nodeDefinitionId(ni.getNodeDefinitionId()).nodeName(ni.getNodeName())
                .nodeType(ni.getNode().getClass().getSimpleName()).triggerTime(ni.getTriggerTime())
                .leaveTime(ni.getLeaveTime()).build();
    }

    protected VariableInstanceEventBody create(ProcessVariableChangedEvent event) {
        VariableInstanceEventBody.Builder eventBuilder = VariableInstanceEventBody.create()
                .changeDate(event.getEventDate()).processId(event.getProcessInstance().getProcessId())
                .processInstanceId(event.getProcessInstance().getId())
                .rootProcessId(event.getProcessInstance().getRootProcessId())
                .rootProcessInstanceId(event.getProcessInstance().getRootProcessInstanceId())
                .variableName(event.getVariableId()).variableValue(event.getNewValue())
                .variablePreviousValue(event.getOldValue());

        if (event.getNodeInstance() != null) {
            eventBuilder.changedByNodeId(event.getNodeInstance().getNodeDefinitionId())
                    .changedByNodeName(event.getNodeInstance().getNodeName())
                    .changedByNodeType(event.getNodeInstance().getNode().getClass().getSimpleName());
        }

        return eventBuilder.build();
    }

    protected String extractRuntimeSource(Map<String, String> metadata) {
        String processId = metadata.get(ProcessInstanceEventBody.PROCESS_ID_META_DATA);
        if (processId == null) {
            return null;
        } else {
            return service + "/"
                    + (processId.contains(".") ? processId.substring(processId.lastIndexOf('.') + 1) : processId);
        }
    }
}
