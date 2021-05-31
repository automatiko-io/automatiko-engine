package io.automatiko.engine.workflow.serverless.parser;

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;
import io.automatiko.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.serverless.parser.core.ServerlessWorkflowFactory;
import io.automatiko.engine.workflow.serverless.parser.util.ServerlessWorkflowUtils;
import io.automatiko.engine.workflow.serverless.parser.util.WorkflowAppContext;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.branches.Branch;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.mapper.BaseObjectMapper;
import io.serverlessworkflow.api.produce.ProduceEvent;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.DelayState;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.ParallelState;
import io.serverlessworkflow.api.states.SubflowState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.api.transitions.Transition;

public class ServerlessWorkflowParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerlessWorkflowParser.class);

    private static final String NODE_START_NAME = "Start";
    private static final String NODE_END_NAME = "End";
    private static final String NODETOID_START = "start";
    private static final String NODETOID_END = "end";
    private static final String XORSPLITDEFAULT = "Default";

    private AtomicLong idCounter = new AtomicLong(1);
    private ServerlessWorkflowFactory factory;
    private BaseObjectMapper objectMapper;

    public ServerlessWorkflowParser(String workflowFormat) {
        this.objectMapper = ServerlessWorkflowUtils.getObjectMapper(workflowFormat);
        this.factory = new ServerlessWorkflowFactory(WorkflowAppContext.ofAppResources());
    }

    public ServerlessWorkflowParser(String workflowFormat, WorkflowAppContext workflowAppContext) {
        this.objectMapper = ServerlessWorkflowUtils.getObjectMapper(workflowFormat);
        this.factory = new ServerlessWorkflowFactory(workflowAppContext);
    }

    public ExecutableProcess parseWorkFlow(Reader workflowFile) throws JsonProcessingException {
        Workflow workflow = objectMapper.readValue(ServerlessWorkflowUtils.readWorkflowFile(workflowFile), Workflow.class);
        ExecutableProcess process = factory.createProcess(workflow);
        Map<String, Map<String, Long>> nameToNodeId = new HashMap<>();
        Map<Long, String> compensationParentToChild = new HashMap<>();
        Map<Long, Error> boundaryIdToError = new HashMap<>();

        if (!ServerlessWorkflowUtils.includesSupportedStates(workflow)) {
            LOGGER.warn("workflow includes currently unsupported states.");
            LOGGER.warn("default process is generated.");

            StartNode startNode = factory.startNode(idCounter.getAndIncrement(), NODE_START_NAME, process);
            EndNode endNode = factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false, process);
            factory.connect(startNode.getId(), endNode.getId(), startNode.getId() + "_" + endNode.getId(), process, false);

            factory.validate(process);
            return process;
        }

        List<State> workflowStates = workflow.getStates();
        List<FunctionDefinition> workflowFunctions = workflow.getFunctions() != null ? workflow.getFunctions().getFunctionDefs()
                : null;

        StartNode workflowStartNode = null;
        String workflowStartStateName = workflow.getStart().getStateName();
        Map<String, EndNode> workflowEndNodes = new HashMap<>();

        if (workflowStartStateName == null || workflowStartStateName.trim().length() < 1) {
            throw new IllegalArgumentException("workflow does not define a starting state");
        }

        State workflowStartState = ServerlessWorkflowUtils.getWorkflowStartState(workflow);

        // starting event states can have multiple starts. this is handled below
        if (!workflowStartState.getType().equals(DefaultState.Type.EVENT)) {
            workflowStartNode = factory.startNode(idCounter.getAndIncrement(), NODE_START_NAME, process);
        }

        List<State> endStates = ServerlessWorkflowUtils.getWorkflowEndStates(workflow);

        for (State endState : endStates) {
            if (endState.getEnd().getProduceEvents() != null && !endState.getEnd().getProduceEvents().isEmpty()) {
                workflowEndNodes.put(endState.getName(), factory.messageEndNode(idCounter.getAndIncrement(), NODE_END_NAME,
                        workflow, endState.getEnd(), process));
            } else {
                workflowEndNodes.put(endState.getName(),
                        factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false, process));
            }
        }

        for (State state : workflowStates) {
            if (state.getType().equals(DefaultState.Type.EVENT)) {
                EventState eventState = (EventState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(idCounter.getAndIncrement(), state.getName(),
                        process);
                handleActions(state, workflowFunctions, eventState.getOnEvents().get(0).getActions(), process,
                        embeddedSubProcess, workflow, boundaryIdToError);

                List<String> onEventRefs = eventState.getOnEvents().get(0).getEventRefs();
                if (onEventRefs.size() == 1) {
                    StartNode singleMessageStartNode = factory.messageStartNode(idCounter.getAndIncrement(),
                            ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                                    eventState.getOnEvents().get(0).getEventRefs().get(0)),
                            process);
                    factory.connect(singleMessageStartNode.getId(), embeddedSubProcess.getId(),
                            singleMessageStartNode.getId() + "_" + embeddedSubProcess.getId(), process, false);
                } else {
                    Join messageStartJoin = factory.joinNode(idCounter.getAndIncrement(), eventState.getName() + "Split",
                            Join.TYPE_XOR, process);

                    for (String onEventRef : onEventRefs) {
                        StartNode messageStartNode = factory.messageStartNode(idCounter.getAndIncrement(),
                                ServerlessWorkflowUtils.getWorkflowEventFor(workflow, onEventRef), process);
                        factory.connect(messageStartNode.getId(), messageStartJoin.getId(),
                                messageStartNode.getId() + "_" + messageStartJoin.getId(), process, false);
                    }
                    factory.connect(messageStartJoin.getId(), embeddedSubProcess.getId(),
                            messageStartJoin.getId() + "_" + embeddedSubProcess.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(embeddedSubProcess.getId(), compensationNode.getId(),
                                embeddedSubProcess.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(embeddedSubProcess.getId(), workflowEndNodes.get(state.getName()).getId(),
                                embeddedSubProcess.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process,
                                false);
                    }
                }

                if (eventState.getCompensatedBy() != null && eventState.getCompensatedBy().trim().length() > 0) {
                    BoundaryEventNode boundaryEventNode = factory.compensationBoundaryEventNode(
                            idCounter.getAndIncrement(), "CompensationBoundary", process, embeddedSubProcess);
                    compensationParentToChild.put(boundaryEventNode.getId(), eventState.getCompensatedBy());
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, embeddedSubProcess.getId());
                startEndMap.put(NODETOID_END, embeddedSubProcess.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }

            if (state.getType().equals(DefaultState.Type.OPERATION)) {
                OperationState operationState = (OperationState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(idCounter.getAndIncrement(), state.getName(),
                        process);
                handleActions(state, workflowFunctions, operationState.getActions(), process, embeddedSubProcess, workflow,
                        boundaryIdToError);

                if (state.getName().equals(workflowStartStateName)) {
                    factory.connect(workflowStartNode.getId(), embeddedSubProcess.getId(),
                            workflowStartNode.getId() + "_" + embeddedSubProcess.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(embeddedSubProcess.getId(), compensationNode.getId(),
                                embeddedSubProcess.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(embeddedSubProcess.getId(), workflowEndNodes.get(state.getName()).getId(),
                                embeddedSubProcess.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process,
                                false);
                    }
                }

                if (operationState.getCompensatedBy() != null && operationState.getCompensatedBy().trim().length() > 0) {
                    BoundaryEventNode boundaryEventNode = factory.compensationBoundaryEventNode(
                            idCounter.getAndIncrement(), "CompensationBoundary", process, embeddedSubProcess);
                    compensationParentToChild.put(boundaryEventNode.getId(), operationState.getCompensatedBy());
                }

                if (operationState.isUsedForCompensation()) {
                    embeddedSubProcess.setMetaData("isForCompensation", true);
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, embeddedSubProcess.getId());
                startEndMap.put(NODETOID_END, embeddedSubProcess.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }

            if (state.getType().equals(DefaultState.Type.DELAY)) {
                DelayState delayState = (DelayState) state;

                TimerNode timerNode = factory.timerNode(idCounter.getAndIncrement(), delayState.getName(),
                        delayState.getTimeDelay(), process);

                if (state.getName().equals(workflowStartStateName)) {
                    factory.connect(workflowStartNode.getId(), timerNode.getId(),
                            workflowStartNode.getId() + "_" + timerNode.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(timerNode.getId(), compensationNode.getId(),
                                timerNode.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(timerNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                timerNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    }
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, timerNode.getId());
                startEndMap.put(NODETOID_END, timerNode.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }

            if (state.getType().equals(DefaultState.Type.INJECT)) {
                InjectState injectState = (InjectState) state;

                ActionNode actionNode;

                JsonNode toInjectNode = injectState.getData();

                if (toInjectNode != null) {
                    actionNode = factory.scriptNode(idCounter.getAndIncrement(), injectState.getName(),
                            ServerlessWorkflowUtils.getInjectScript(toInjectNode), process);
                } else {
                    //no-op script
                    actionNode = factory.scriptNode(idCounter.getAndIncrement(), injectState.getName(), "", process);
                }

                if (state.getName().equals(workflowStartStateName)) {
                    factory.connect(workflowStartNode.getId(), actionNode.getId(),
                            workflowStartNode.getId() + "_" + actionNode.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(actionNode.getId(), compensationNode.getId(),
                                actionNode.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(actionNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                actionNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    }
                }

                if (injectState.isUsedForCompensation()) {
                    actionNode.setMetaData("isForCompensation", true);
                }

                if (injectState.getCompensatedBy() != null && injectState.getCompensatedBy().trim().length() > 0) {
                    BoundaryEventNode boundaryEventNode = factory.compensationBoundaryEventNode(
                            idCounter.getAndIncrement(), "CompensationBoundary", process, actionNode);
                    compensationParentToChild.put(boundaryEventNode.getId(), injectState.getCompensatedBy());
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, actionNode.getId());
                startEndMap.put(NODETOID_END, actionNode.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }

            if (state.getType().equals(DefaultState.Type.SUBFLOW)) {
                SubflowState subflowState = (SubflowState) state;

                SubProcessNode callActivityNode = factory.callActivity(idCounter.getAndIncrement(), subflowState.getName(),
                        subflowState.getWorkflowId(), subflowState.isWaitForCompletion(), process);

                if (state.getName().equals(workflowStartStateName)) {
                    factory.connect(workflowStartNode.getId(), callActivityNode.getId(),
                            workflowStartNode.getId() + "_" + callActivityNode.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(callActivityNode.getId(), compensationNode.getId(),
                                callActivityNode.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(callActivityNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                callActivityNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    }
                }

                if (subflowState.isUsedForCompensation()) {
                    callActivityNode.setMetaData("isForCompensation", true);
                }

                if (subflowState.getCompensatedBy() != null && subflowState.getCompensatedBy().trim().length() > 0) {
                    BoundaryEventNode boundaryEventNode = factory.compensationBoundaryEventNode(
                            idCounter.getAndIncrement(), "CompensationBoundary", process, callActivityNode);
                    compensationParentToChild.put(boundaryEventNode.getId(), subflowState.getCompensatedBy());
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, callActivityNode.getId());
                startEndMap.put(NODETOID_END, callActivityNode.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }

            if (state.getType().equals(DefaultState.Type.SWITCH)) {
                SwitchState switchState = (SwitchState) state;

                // check if data-based or event-based switch state
                if (switchState.getDataConditions() != null && !switchState.getDataConditions().isEmpty()) {
                    // data-based switch state
                    Split splitNode = factory.splitNode(idCounter.getAndIncrement(), switchState.getName(), Split.TYPE_XOR,
                            process);

                    if (state.getName().equals(workflowStartStateName)) {
                        factory.connect(workflowStartNode.getId(), splitNode.getId(),
                                workflowStartNode.getId() + "_" + splitNode.getId(), process, false);
                    }
                    // switch states cannot be end states

                    Map<String, Long> startEndMap = new HashMap<>();
                    startEndMap.put(NODETOID_START, splitNode.getId());
                    startEndMap.put(NODETOID_END, splitNode.getId());
                    nameToNodeId.put(state.getName(), startEndMap);
                } else if (switchState.getEventConditions() != null && !switchState.getEventConditions().isEmpty()) {
                    // event-based switch state
                    Split splitNode = factory.eventBasedSplit(idCounter.getAndIncrement(), switchState.getName(), process);
                    if (state.getName().equals(workflowStartStateName)) {
                        factory.connect(workflowStartNode.getId(), splitNode.getId(),
                                workflowStartNode.getId() + "_" + splitNode.getId(), process, false);
                    }
                    // switch states cannot be end states

                    Map<String, Long> startEndMap = new HashMap<>();
                    startEndMap.put(NODETOID_START, splitNode.getId());
                    startEndMap.put(NODETOID_END, splitNode.getId());
                    nameToNodeId.put(state.getName(), startEndMap);
                } else {
                    LOGGER.warn("unable to determine switch state type (data or event based): {}", switchState.getName());
                }
            }

            if (state.getType().equals(DefaultState.Type.PARALLEL)) {
                ParallelState parallelState = (ParallelState) state;

                Split parallelSplit = factory.splitNode(idCounter.getAndIncrement(), parallelState.getName() + NODE_START_NAME,
                        Split.TYPE_AND, process);
                Join parallelJoin = factory.joinNode(idCounter.getAndIncrement(), parallelState.getName() + NODE_END_NAME,
                        Join.TYPE_AND, process);

                for (Branch branch : parallelState.getBranches()) {
                    String subflowStateId = branch.getWorkflowId();
                    SubProcessNode callActivityNode = factory.callActivity(idCounter.getAndIncrement(), branch.getName(),
                            subflowStateId, true, process);

                    factory.connect(parallelSplit.getId(), callActivityNode.getId(),
                            parallelSplit.getId() + "_" + callActivityNode.getId(), process, false);
                    factory.connect(callActivityNode.getId(), parallelJoin.getId(),
                            callActivityNode.getId() + "_" + parallelJoin.getId(), process, false);

                }

                if (state.getName().equals(workflowStartStateName)) {
                    factory.connect(workflowStartNode.getId(), parallelSplit.getId(),
                            workflowStartNode.getId() + "_" + parallelSplit.getId(), process, false);
                }

                if (state.getEnd() != null) {
                    if (state.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(parallelJoin.getId(), compensationNode.getId(),
                                parallelJoin.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), workflowEndNodes.get(state.getName()).getId(),
                                compensationNode.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    } else {
                        factory.connect(parallelJoin.getId(), workflowEndNodes.get(state.getName()).getId(),
                                parallelJoin.getId() + "_" + workflowEndNodes.get(state.getName()).getId(), process, false);
                    }
                }

                Map<String, Long> startEndMap = new HashMap<>();
                startEndMap.put(NODETOID_START, parallelSplit.getId());
                startEndMap.put(NODETOID_END, parallelJoin.getId());
                nameToNodeId.put(state.getName(), startEndMap);
            }
        }

        workflow.getStates().stream().filter(state -> (state instanceof State)).forEach(state -> {
            Transition transition = state.getTransition();

            if (transition != null && transition.getNextState() != null) {
                Long sourceId = nameToNodeId.get(state.getName()).get(NODETOID_END);
                Long targetId = nameToNodeId.get(state.getTransition().getNextState()).get(NODETOID_START);

                if (!transition.getProduceEvents().isEmpty()) {
                    if (transition.getProduceEvents().size() == 1) {
                        ActionNode sendEventNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                                        transition.getProduceEvents().get(0).getEventRef()),
                                process);
                        factory.connect(sourceId, sendEventNode.getId(), sourceId + "_" + sendEventNode.getId(), process,
                                false);
                        factory.connect(sendEventNode.getId(), targetId, sendEventNode + "_" + targetId, process, false);
                    } else {
                        ActionNode firstActionNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                                        transition.getProduceEvents().get(0).getEventRef()),
                                process);
                        ActionNode lastActionNode = null;
                        for (ProduceEvent p : transition.getProduceEvents().subList(1, transition.getProduceEvents().size())) {
                            ActionNode newActionNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                    ServerlessWorkflowUtils.getWorkflowEventFor(workflow, p.getEventRef()), process);
                            if (lastActionNode == null) {
                                lastActionNode = newActionNode;
                                factory.connect(firstActionNode.getId(), lastActionNode.getId(),
                                        firstActionNode.getId() + "_" + lastActionNode.getId(), process, false);
                            } else {
                                factory.connect(lastActionNode.getId(), newActionNode.getId(),
                                        lastActionNode.getId() + "_" + newActionNode.getId(), process, false);
                                lastActionNode = newActionNode;
                            }
                        }
                        factory.connect(sourceId, firstActionNode.getId(), sourceId + "_" + firstActionNode.getId(), process,
                                false);
                        factory.connect(lastActionNode.getId(), targetId, lastActionNode + "_" + targetId, process, false);
                    }
                } else {
                    if (transition.isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(sourceId, compensationNode.getId(), sourceId + "_" + compensationNode.getId(), process,
                                false);
                        factory.connect(compensationNode.getId(), targetId, compensationNode.getId() + "_" + targetId, process,
                                false);
                    } else {
                        factory.connect(sourceId, targetId, sourceId + "_" + targetId, process, false);
                    }
                }
            }
        });

        // after all nodes initialized add constraints, finish switch nodes
        List<State> switchStates = ServerlessWorkflowUtils.getStatesByType(workflow, DefaultState.Type.SWITCH);
        if (switchStates != null && switchStates.size() > 0) {
            for (State state : switchStates) {
                SwitchState switchState = (SwitchState) state;

                if (switchState.getDataConditions() != null && !switchState.getDataConditions().isEmpty()) {
                    finalizeDataBasedSwitchState(switchState, nameToNodeId, process, workflow);
                } else {
                    finalizeEventBasedSwitchState(switchState, nameToNodeId, process, workflow);
                }
            }
        }

        // finish connecting boundary events
        for (Map.Entry<Long, Error> entry : boundaryIdToError.entrySet()) {
            Long boundaryEventId = entry.getKey();
            Error errorObj = entry.getValue();

            if (errorObj.getTransition() != null) {
                long targetId = nameToNodeId.get(errorObj.getTransition().getNextState()).get(NODETOID_START);
                if (errorObj.getTransition().isCompensate()) {
                    ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                            process, process);
                    factory.connect(boundaryEventId, compensationNode.getId(), boundaryEventId + "_" + compensationNode.getId(),
                            process, false);
                    factory.connect(compensationNode.getId(), targetId, compensationNode.getId() + "_" + targetId, process,
                            false);
                } else {
                    factory.connect(boundaryEventId, targetId, boundaryEventId + "_" + targetId, process, false);
                }
            }

            if (errorObj.getEnd() != null) {
                if (errorObj.getEnd().getProduceEvents() != null && !errorObj.getEnd().getProduceEvents().isEmpty()) {
                    EndNode boundaryEndNode = factory.messageEndNode(idCounter.getAndIncrement(), NODE_END_NAME, workflow,
                            errorObj.getEnd(), process);
                    if (errorObj.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(boundaryEventId, compensationNode.getId(),
                                boundaryEventId + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), boundaryEndNode.getId(),
                                compensationNode.getId() + "_" + boundaryEndNode.getId(), process, false);
                    } else {
                        factory.connect(boundaryEventId, boundaryEndNode.getId(),
                                boundaryEventId + "_" + boundaryEndNode.getId(), process, false);
                    }
                } else {
                    EndNode boundaryEndNode = factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false, process);
                    if (errorObj.getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(boundaryEventId, compensationNode.getId(), boundaryEventId + "_" + compensationNode,
                                process, false);
                        factory.connect(compensationNode.getId(), boundaryEndNode.getId(),
                                compensationNode.getId() + "_" + boundaryEndNode.getId(), process, false);
                    } else {
                        factory.connect(boundaryEventId, boundaryEndNode.getId(),
                                boundaryEventId + "_" + boundaryEndNode.getId(), process, false);
                    }
                }
            }
        }

        // finish connecting compensations
        for (Map.Entry<Long, String> entry : compensationParentToChild.entrySet()) {
            Long parentNodeId = entry.getKey();
            String parentCompensationNode = entry.getValue();

            Long parentCompensationNodeId = nameToNodeId.get(parentCompensationNode).get(NODETOID_END);

            factory.connect(parentNodeId, parentCompensationNodeId, "", process, true);
        }

        factory.validate(process);

        if (workflow.getExecTimeout() != null) {
            factory.addExecutionTimeout(idCounter.get(), workflow.getExecTimeout(), process);
        }

        return process;
    }

    protected void finalizeEventBasedSwitchState(SwitchState switchState, Map<String, Map<String, Long>> nameToNodeId,
            ExecutableProcess process, Workflow workflow) {
        long eventSplitNodeId = nameToNodeId.get(switchState.getName()).get(NODETOID_START);
        Split eventSplit = (Split) process.getNode(eventSplitNodeId);

        if (eventSplit != null) {

            List<EventCondition> conditions = switchState.getEventConditions();

            if (conditions != null && !conditions.isEmpty()) {
                for (EventCondition eventCondition : conditions) {
                    EventDefinition eventDefinition = ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                            eventCondition.getEventRef());

                    if (eventCondition.getTransition() != null) {
                        long targetId = nameToNodeId.get(eventCondition.getTransition().getNextState()).get(NODETOID_START);

                        EventNode eventNode = factory.consumeEventNode(idCounter.getAndIncrement(), eventDefinition, process);

                        factory.connect(eventSplit.getId(), eventNode.getId(), eventSplit.getId() + "_" + eventNode.getId(),
                                process, false);
                        factory.connect(eventNode.getId(), targetId, eventNode.getId() + "_" + targetId, process, false);
                    } else if (eventCondition.getEnd() != null) {
                        EventNode eventNode = factory.consumeEventNode(idCounter.getAndIncrement(), eventDefinition, process);
                        EndNode conditionEndNode = factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false, process);
                        if (eventCondition.getEnd().isCompensate()) {
                            ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(),
                                    "Compensation", process, process);
                            factory.connect(eventSplit.getId(), eventNode.getId(), eventSplit.getId() + "_" + eventNode.getId(),
                                    process, false);
                            factory.connect(eventNode.getId(), compensationNode.getId(),
                                    eventNode.getId() + "_" + compensationNode.getId(), process, false);
                            factory.connect(compensationNode.getId(), conditionEndNode.getId(),
                                    compensationNode.getId() + "_" + conditionEndNode.getId(), process, false);
                        } else {
                            factory.connect(eventSplit.getId(), eventNode.getId(), eventSplit.getId() + "_" + eventNode.getId(),
                                    process, false);
                            factory.connect(eventNode.getId(), conditionEndNode.getId(),
                                    eventNode.getId() + "_" + conditionEndNode.getId(), process, false);
                        }
                    }

                }
            } else {
                LOGGER.warn("switch state has no event conditions: {}", switchState.getName());
            }

        } else {
            LOGGER.error("unable to get event split node for switch state: {}", switchState.getName());
        }
    }

    protected void finalizeDataBasedSwitchState(SwitchState switchState, Map<String, Map<String, Long>> nameToNodeId,
            ExecutableProcess process, Workflow workflow) {
        long splitNodeId = nameToNodeId.get(switchState.getName()).get(NODETOID_START);
        Split xorSplit = (Split) process.getNode(splitNodeId);

        if (xorSplit != null) {
            // set default connection
            // 1. if its a transition
            if (switchState.getDefault() != null && switchState.getDefault().getTransition() != null
                    && switchState.getDefault().getTransition().getNextState() != null) {
                long targetId = nameToNodeId.get(switchState.getDefault().getTransition().getNextState()).get(NODETOID_START);
                xorSplit.getMetaData().put(XORSPLITDEFAULT, xorSplit.getId() + "_" + targetId);
            }
            // 2. if its an end
            if (switchState.getDefault() != null && switchState.getDefault().getEnd() != null) {
                if (switchState.getEnd().getProduceEvents() != null && !switchState.getEnd().getProduceEvents().isEmpty()) {
                    EndNode defaultEndNode = factory.messageEndNode(idCounter.getAndIncrement(), NODE_END_NAME, workflow,
                            switchState.getDefault().getEnd(), process);
                    if (switchState.getDefault().getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(xorSplit.getId(), compensationNode.getId(),
                                xorSplit.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), defaultEndNode.getId(),
                                compensationNode.getId() + "_" + defaultEndNode.getId(), process, false);
                        xorSplit.getMetaData().put(XORSPLITDEFAULT, xorSplit.getId() + "_" + compensationNode.getId());
                    } else {
                        factory.connect(xorSplit.getId(), defaultEndNode.getId(),
                                xorSplit.getId() + "_" + defaultEndNode.getId(), process, false);
                        xorSplit.getMetaData().put(XORSPLITDEFAULT, xorSplit.getId() + "_" + defaultEndNode.getId());
                    }
                } else {
                    EndNode defaultEndNode = factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false, process);
                    if (switchState.getDefault().getEnd().isCompensate()) {
                        ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(), "Compensation",
                                process, process);
                        factory.connect(xorSplit.getId(), compensationNode.getId(),
                                xorSplit.getId() + "_" + compensationNode.getId(), process, false);
                        factory.connect(compensationNode.getId(), defaultEndNode.getId(),
                                compensationNode.getId() + "_" + defaultEndNode.getId(), process, false);
                        xorSplit.getMetaData().put(XORSPLITDEFAULT, xorSplit.getId() + "_" + compensationNode.getId());
                    } else {
                        factory.connect(xorSplit.getId(), defaultEndNode.getId(),
                                xorSplit.getId() + "_" + defaultEndNode.getId(), process, false);
                        xorSplit.getMetaData().put(XORSPLITDEFAULT, xorSplit.getId() + "_" + defaultEndNode.getId());
                    }
                }
            }

            List<DataCondition> conditions = switchState.getDataConditions();

            if (conditions != null && !conditions.isEmpty()) {
                for (DataCondition condition : conditions) {
                    long targetId = 0;
                    if (condition.getTransition() != null) {
                        // check if we need to produce an event in-between
                        if (!condition.getTransition().getProduceEvents().isEmpty()) {

                            if (condition.getTransition().getProduceEvents().size() == 1) {
                                ActionNode sendEventNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                        ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                                                condition.getTransition().getProduceEvents().get(0).getEventRef()),
                                        process);

                                long nextStateId = nameToNodeId.get(condition.getTransition().getNextState())
                                        .get(NODETOID_START);
                                factory.connect(xorSplit.getId(), sendEventNode.getId(),
                                        xorSplit.getId() + "_" + sendEventNode.getId(), process, false);
                                factory.connect(sendEventNode.getId(), nextStateId, sendEventNode.getId() + "_" + nextStateId,
                                        process, false);

                                targetId = sendEventNode.getId();
                            } else {
                                ActionNode firstActionNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                        ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                                                condition.getTransition().getProduceEvents().get(0).getEventRef()),
                                        process);
                                ActionNode lastActionNode = null;
                                for (ProduceEvent p : condition.getTransition().getProduceEvents().subList(1,
                                        condition.getTransition().getProduceEvents().size())) {
                                    ActionNode newActionNode = factory.sendEventNode(idCounter.getAndIncrement(),
                                            ServerlessWorkflowUtils.getWorkflowEventFor(workflow, p.getEventRef()), process);
                                    if (lastActionNode == null) {
                                        lastActionNode = newActionNode;
                                        factory.connect(firstActionNode.getId(), lastActionNode.getId(),
                                                firstActionNode.getId() + "_" + lastActionNode.getId(), process, false);
                                    } else {
                                        factory.connect(lastActionNode.getId(), newActionNode.getId(),
                                                lastActionNode.getId() + "_" + newActionNode.getId(), process, false);
                                        lastActionNode = newActionNode;
                                    }
                                }

                                long nextStateId = nameToNodeId.get(condition.getTransition().getNextState())
                                        .get(NODETOID_START);
                                factory.connect(xorSplit.getId(), firstActionNode.getId(),
                                        xorSplit.getId() + "_" + firstActionNode.getId(), process, false);
                                factory.connect(lastActionNode.getId(), nextStateId, lastActionNode.getId() + "_" + nextStateId,
                                        process, false);

                                targetId = firstActionNode.getId();
                            }
                        } else {
                            targetId = nameToNodeId.get(condition.getTransition().getNextState()).get(NODETOID_START);
                            if (condition.getTransition().isCompensate()) {
                                ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(),
                                        "Compensation", process, process);
                                factory.connect(xorSplit.getId(), compensationNode.getId(),
                                        xorSplit.getId() + "_" + compensationNode.getId(), process, false);
                                factory.connect(compensationNode.getId(), targetId, compensationNode.getId() + "_" + targetId,
                                        process, false);
                            } else {
                                factory.connect(xorSplit.getId(), targetId, xorSplit.getId() + "_" + targetId, process, false);
                            }
                        }
                    } else if (condition.getEnd() != null) {
                        if (condition.getEnd().getProduceEvents() != null && !condition.getEnd().getProduceEvents().isEmpty()) {
                            EndNode conditionEndNode = factory.messageEndNode(idCounter.getAndIncrement(), NODE_END_NAME,
                                    workflow, condition.getEnd(), process);
                            if (condition.getEnd().isCompensate()) {
                                ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(),
                                        "Compensation", process, process);
                                factory.connect(xorSplit.getId(), compensationNode.getId(),
                                        xorSplit.getId() + "_" + compensationNode.getId(), process, false);
                                factory.connect(compensationNode.getId(), conditionEndNode.getId(),
                                        compensationNode.getId() + "_" + conditionEndNode.getId(), process, false);
                                targetId = compensationNode.getId();
                            } else {
                                factory.connect(xorSplit.getId(), conditionEndNode.getId(),
                                        xorSplit.getId() + "_" + conditionEndNode.getId(), process, false);
                                targetId = conditionEndNode.getId();
                            }
                        } else {
                            EndNode conditionEndNode = factory.endNode(idCounter.getAndIncrement(), NODE_END_NAME, false,
                                    process);
                            if (condition.getEnd().isCompensate()) {
                                ActionNode compensationNode = factory.compensationEventNode(idCounter.getAndIncrement(),
                                        "Compensation", process, process);
                                factory.connect(xorSplit.getId(), compensationNode.getId(),
                                        xorSplit.getId() + "_" + compensationNode.getId(), process, false);
                                factory.connect(compensationNode.getId(), conditionEndNode.getId(),
                                        compensationNode.getId() + "_" + conditionEndNode.getId(), process, false);
                                targetId = compensationNode.getId();
                            } else {
                                factory.connect(xorSplit.getId(), conditionEndNode.getId(),
                                        xorSplit.getId() + "_" + conditionEndNode.getId(), process, false);
                                targetId = conditionEndNode.getId();
                            }
                        }
                    }

                    // set constraint
                    boolean isDefaultConstraint = false;

                    if (switchState.getDefault() != null && switchState.getDefault().getTransition() != null
                            && condition.getTransition() != null &&
                            condition.getTransition().getNextState()
                                    .equals(switchState.getDefault().getTransition().getNextState())) {
                        isDefaultConstraint = true;
                    }

                    if (switchState.getDefault() != null && switchState.getDefault().getEnd() != null
                            && condition.getEnd() != null) {
                        isDefaultConstraint = true;
                    }

                    ConstraintImpl constraintImpl = factory.splitConstraint(xorSplit.getId() + "_" + targetId,
                            "DROOLS_DEFAULT", "java", ServerlessWorkflowUtils.conditionScript(condition.getCondition()), 0,
                            isDefaultConstraint);
                    xorSplit.addConstraint(
                            new ConnectionRef(xorSplit.getId() + "_" + targetId, targetId, Node.CONNECTION_DEFAULT_TYPE),
                            constraintImpl);

                }
            } else {
                LOGGER.warn("switch state has no conditions: {}", switchState.getName());
            }
        } else {
            LOGGER.error("unable to get split node for switch state: {}", switchState.getName());
        }
    }

    protected void handleActions(State state, List<FunctionDefinition> workflowFunctions, List<Action> actions,
            ExecutableProcess process, CompositeContextNode embeddedSubProcess, Workflow workflow,
            Map<Long, Error> boundaryIdToError) {
        if (workflowFunctions != null && actions != null && !actions.isEmpty()) {
            StartNode embeddedStartNode = factory.startNode(idCounter.getAndIncrement(), "EmbeddedStart", embeddedSubProcess);
            Node start = embeddedStartNode;
            Node current = null;

            for (Action action : actions) {

                FunctionDefinition actionFunction;

                Optional<FunctionDefinition> optionalActionFunction = workflowFunctions.stream()
                        .filter(wf -> wf.getName().equals(action.getFunctionRef().getRefName()))
                        .findFirst();

                actionFunction = optionalActionFunction.isPresent() ? optionalActionFunction.get() : null;

                if (actionFunction != null && actionFunction.getOperation() != null) {

                    if (actionFunction.getType().equals(FunctionDefinition.Type.EXPRESSION)) {
                        StringBuilder operation = new StringBuilder(actionFunction.getOperation());
                        operation.append("(");
                        JsonNode params = action.getFunctionRef().getArguments();

                        if (params != null) {
                            Iterator<String> it = params.fieldNames();
                            while (it.hasNext()) {

                                String name = it.next();
                                String value = params.get(name).asText();
                                if (value.equals("$..*")) {
                                    value = "workflowdata";
                                }
                                operation.append("context.getVariable(\"" + value + "\")");
                            }
                        }
                        operation.append(")");
                        current = factory.scriptNode(idCounter.getAndIncrement(), action.getName(),
                                operation.toString(), embeddedSubProcess);
                    } else if (actionFunction.getType().equals(FunctionDefinition.Type.REST)) {

                        current = factory.serviceNode(idCounter.getAndIncrement(), action,
                                actionFunction, embeddedSubProcess);

                    }
                    factory.connect(start.getId(), current.getId(), start.getId() + "_" + current.getId(),
                            embeddedSubProcess,
                            false);
                    if (state.getMetadata() != null) {
                        current.getMetaData().putAll(state.getMetadata());
                    }
                    start = current;
                } else {
                    LOGGER.error("Invalid action function reference: {}" + actionFunction);
                }
            }
            EndNode embeddedEndNode = factory.endNode(idCounter.getAndIncrement(), "EmbeddedEnd", false, embeddedSubProcess);
            try {
                factory.connect(current.getId(), embeddedEndNode.getId(), current.getId() + "_" + embeddedEndNode.getId(),
                        embeddedSubProcess, false);
            } catch (NullPointerException e) {
                LOGGER.warn("unable to connect current node to embedded end node");
            }

            // error / retry handling - add to the subprocess level
            if (state.getOnErrors() != null && state.getOnErrors().size() > 0) {
                addErrorHandlingAndRetries(state.getOnErrors(), process, workflow, embeddedSubProcess, boundaryIdToError);
            }
        } else {
            // no actions: start->end
            StartNode embeddedStartNode = factory.startNode(idCounter.getAndIncrement(), "EmbeddedStart", embeddedSubProcess);
            EndNode embeddedEndNode = factory.endNode(idCounter.getAndIncrement(), "EmbeddedEnd", false, embeddedSubProcess);
            factory.connect(embeddedStartNode.getId(), embeddedEndNode.getId(),
                    embeddedStartNode.getId() + "_" + embeddedEndNode.getId(), embeddedSubProcess, false);
        }
    }

    protected void addErrorHandlingAndRetries(List<Error> errorDefs, ExecutableProcess process, Workflow workflow,
            CompositeContextNode embeddedSubProcess, Map<Long, Error> boundaryIdToError) {
        for (Error errorDef : errorDefs) {
            BoundaryEventNode boundaryEventNode = factory.errorBoundaryEventNode(idCounter.getAndIncrement(), errorDef, process,
                    embeddedSubProcess, workflow);
            boundaryIdToError.put(boundaryEventNode.getId(), errorDef);
        }
    }
}