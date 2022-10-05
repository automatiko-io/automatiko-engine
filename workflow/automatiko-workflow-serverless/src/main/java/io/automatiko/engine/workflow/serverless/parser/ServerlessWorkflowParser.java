package io.automatiko.engine.workflow.serverless.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.instance.impl.ReturnValueConstraintEvaluator;
import io.automatiko.engine.workflow.base.instance.impl.jq.InputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.JqReturnValueEvaluator;
import io.automatiko.engine.workflow.base.instance.impl.jq.OutputJqAssignmentAction;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.branches.Branch;
import io.serverlessworkflow.api.error.ErrorDefinition;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.events.OnEvents;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.functions.SubFlowRef.Invoke;
import io.serverlessworkflow.api.functions.SubFlowRef.OnParentComplete;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.produce.ProduceEvent;
import io.serverlessworkflow.api.retry.RetryDefinition;
import io.serverlessworkflow.api.states.CallbackState;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.DefaultState.Type;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.OperationState.ActionMode;
import io.serverlessworkflow.api.states.ParallelState;
import io.serverlessworkflow.api.states.ParallelState.CompletionType;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.utils.WorkflowUtils;

public class ServerlessWorkflowParser {

    public Process parse(Reader workflowFile) {

        AtomicLong ids = new AtomicLong(0);
        Workflow workflow = Workflow.fromSource(toString(workflowFile));

        ServerlessWorkflowFactory factory = new ServerlessWorkflowFactory();
        if (!"jq".equalsIgnoreCase(workflow.getExpressionLang())) {
            throw new IllegalArgumentException("Not supported expression language, only 'jq' is supported");
        }
        WorkflowProcess process = factory.createProcess(workflow);

        Map<String, List<String>> diagram = new LinkedHashMap<>();
        process.setMetaData("DiagramInfo", diagram);

        State start = WorkflowUtils.getStartingState(workflow);
        Node startNode;
        if (start.getType().equals(DefaultState.Type.EVENT)) {
            List<Node> nodes = new ArrayList<>();
            EventState eventState = (EventState) start;

            for (OnEvents onEvent : eventState.getOnEvents()) {

                if (eventState.isExclusive()) {
                    // use event based gateway

                    Join join = factory.joinNode(ids.getAndIncrement(), "join_" + eventState.getName(), Join.TYPE_XOR,
                            process);

                    for (String eventRef : onEvent.getEventRefs()) {

                        EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                                .filter(e -> e.getName().equals(eventRef)).findFirst().get();

                        StartNode startMessageNode = factory.messageStartNode(ids.getAndIncrement(), event, onEvent,
                                process);

                        factory.connect(startMessageNode.getId(), join.getId(),
                                "connection_" + startMessageNode.getId() + "_" + join.getId(), process,
                                false);
                    }

                    buildActionsForState(workflow, onEvent.getActions(), process, factory, ids,
                            (first, last) -> {
                                factory.connect(join.getId(), first.getId(),
                                        join.getId() + "_" + first.getId(),
                                        process, false);

                                nodes.add(last);
                            }, (first, last) -> {
                            }, false);
                } else {
                    // use parallel gateway   
                    Join parallelJoin = factory.joinNode(ids.getAndIncrement(), "join_" + eventState.getName(), Join.TYPE_AND,
                            process);

                    for (String eventRef : onEvent.getEventRefs()) {

                        EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                                .filter(e -> e.getName().equals(eventRef)).findFirst().get();

                        StartNode startMessageNode = factory.messageStartNode(ids.getAndIncrement(), event, onEvent,
                                process);
                        factory.connect(startMessageNode.getId(), parallelJoin.getId(),
                                "connection_" + startMessageNode.getId() + "_" + parallelJoin.getId(), process,
                                false);
                    }

                    buildActionsForState(workflow, onEvent.getActions(), process, factory, ids,
                            (first, last) -> {
                                factory.connect(parallelJoin.getId(), first.getId(),
                                        parallelJoin.getId() + "_" + first.getId(),
                                        process, false);

                                nodes.add(last);
                            }, (first, last) -> {
                            }, false);

                }
            }

            // start node becomes the last node after message event handling so other parts are connected to it
            startNode = nodes.get(0);

            if (eventState.getStateDataFilter() != null && eventState.getStateDataFilter().getOutput() != null) {

                ActionNode stateDataFilterActionNode = factory.stateDataFilterActionNode(ids.getAndIncrement(), "", process,
                        eventState.getStateDataFilter().getOutput());

                factory.connect(startNode.getId(), stateDataFilterActionNode.getId(),
                        "connection_" + startNode.getId() + "_" + stateDataFilterActionNode.getId(), process, false);

                startNode = stateDataFilterActionNode;
            }
            if (eventState.getEnd() != null) {

                EndNode endNode = factory.endNode(ids.getAndIncrement(), eventState.getName() + "-end",
                        eventState.getEnd().isTerminate(),
                        process);
                if (eventState.getEnd().getProduceEvents() != null && !eventState.getEnd().getProduceEvents().isEmpty()) {
                    produceEvents(eventState.getEnd().getProduceEvents(), factory, workflow, ids, process,
                            startNode.getId(),
                            endNode.getId());
                } else {
                    factory.connect(startNode.getId(), endNode.getId(),
                            "connection_" + startNode.getId() + "_" + endNode.getId(), process, false);
                }
            }

        } else {
            startNode = factory.startNode(ids.getAndIncrement(), start.getName() + "-start", process);
        }

        // map of state names to node ids for connecting purpose
        Map<String, Long> mappedNodes = new LinkedHashMap<>();
        Node currentNode = null;
        // process all states and create proper node representation for each state
        for (State state : workflow.getStates()) {

            if (state.getType().equals(Type.INJECT)) {

                ActionNode actionNode = factory.injectStateNode(ids.getAndIncrement(), state.getName(), process,
                        ((InjectState) state).getData().toString());
                mappedNodes.put(state.getName(), actionNode.getId());
                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);

                    if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                actionNode.getId(),
                                endNode.getId());
                    } else {
                        factory.connect(actionNode.getId(), endNode.getId(),
                                "connection_" + actionNode.getId() + "_" + endNode.getId(), process, false);
                    }
                }

                currentNode = actionNode;
            } else if (state.getType().equals(DefaultState.Type.OPERATION)) {
                OperationState operationState = (OperationState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                        process);
                currentNode = embeddedSubProcess;

                // handle state data inputs
                Assignment inputAssignment = new Assignment("jq", "", "");
                inputAssignment.setMetaData("Action", new InputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getInput())));
                embeddedSubProcess.addInAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(inputAssignment), null));

                // handle state data outputs
                Assignment outputAssignment = new Assignment("jq", "", "");
                outputAssignment.setMetaData("Action", new OutputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getOutput())));
                embeddedSubProcess.addOutAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(outputAssignment), null));
                mappedNodes.put(state.getName(), embeddedSubProcess.getId());

                StartNode embeddedStartNode = factory.startNode(ids.getAndIncrement(), "EmbeddedStart", embeddedSubProcess);
                EndNode embeddedEndNode = factory.endNode(ids.getAndIncrement(), "EmbeddedEnd", false, embeddedSubProcess);

                if (operationState.getActions() == null || operationState.getActions().isEmpty()) {
                    factory.connect(embeddedStartNode.getId(), embeddedEndNode.getId(),
                            embeddedStartNode.getId() + "_" + embeddedEndNode.getId(),
                            embeddedSubProcess, false);

                    if (state.getEnd() != null) {

                        EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                                state.getEnd().isTerminate(),
                                process);
                        if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                            produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                    embeddedSubProcess.getId(),
                                    endNode.getId());
                        } else {
                            factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                    "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                        }
                    }
                    // ensure that start node is connected
                    if (state.equals(start) && currentNode != null) {
                        factory.connect(startNode.getId(), currentNode.getId(),
                                "connection_" + startNode.getId() + "_" + currentNode.getId(), process, false);
                    }
                    continue;
                }

                if (operationState.getActionMode() == null || operationState.getActionMode() == ActionMode.SEQUENTIAL) {

                    buildActionsForState(workflow, operationState.getActions(), embeddedSubProcess, factory, ids,
                            (first, last) -> {
                                factory.connect(embeddedStartNode.getId(), first.getId(),
                                        embeddedStartNode.getId() + "_" + first.getId(),
                                        embeddedSubProcess, false);

                                factory.connect(last.getId(), embeddedEndNode.getId(),
                                        last.getId() + "_" + embeddedEndNode.getId(),
                                        embeddedSubProcess, false);
                            }, (first, last) -> {
                            }, false);

                } else {
                    Split split = factory.splitNode(ids.getAndIncrement(), "parallel-split-" + state.getName(), Split.TYPE_AND,
                            embeddedSubProcess);

                    Join join = factory.joinNode(ids.getAndIncrement(), "parallel-join-" + state.getName(), Join.TYPE_AND,
                            embeddedSubProcess);

                    factory.connect(embeddedStartNode.getId(), split.getId(),
                            embeddedStartNode.getId() + "_" + split.getId(),
                            embeddedSubProcess, false);

                    factory.connect(join.getId(), embeddedEndNode.getId(), join.getId() + "_" + embeddedEndNode.getId(),
                            embeddedSubProcess, false);

                    buildActionsForState(workflow, operationState.getActions(), embeddedSubProcess, factory, ids,
                            (first, last) -> {

                            }, (first, last) -> {
                                factory.connect(split.getId(), first.getId(),
                                        split.getId() + "_" + first.getId(),
                                        embeddedSubProcess, false);

                                factory.connect(last.getId(), join.getId(), last.getId() + "_" + join.getId(),
                                        embeddedSubProcess, false);
                            }, true);
                }

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                embeddedSubProcess.getId(),
                                endNode.getId());
                    } else {
                        factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                    }
                }
            } else if (state.getType().equals(DefaultState.Type.EVENT)) {

                EventState eventState = (EventState) state;

                if (eventState.equals(start)) {
                    // event state that is start node is already handled
                    continue;
                }

                for (OnEvents onEvent : eventState.getOnEvents()) {

                    CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                            process);

                    currentNode = embeddedSubProcess;
                    // handle state data inputs
                    Assignment inputAssignment = new Assignment("jq", "", "");
                    inputAssignment.setMetaData("Action", new InputJqAssignmentAction(
                            state.getStateDataFilter() == null ? null
                                    : factory.unwrapExpression(state.getStateDataFilter().getInput())));
                    embeddedSubProcess.addInAssociation(
                            new DataAssociation(Collections.emptyList(), "", Arrays.asList(inputAssignment), null));

                    // handle state data outputs
                    Assignment outputAssignment = new Assignment("jq", "", "");
                    outputAssignment.setMetaData("Action", new OutputJqAssignmentAction(
                            state.getStateDataFilter() == null ? null
                                    : factory.unwrapExpression(state.getStateDataFilter().getOutput())));
                    embeddedSubProcess.addOutAssociation(
                            new DataAssociation(Collections.emptyList(), "", Arrays.asList(outputAssignment), null));
                    mappedNodes.put(state.getName(), embeddedSubProcess.getId());

                    StartNode embeddedStartNode = factory.startNode(ids.getAndIncrement(), "EmbeddedStart",
                            embeddedSubProcess);
                    EndNode embeddedEndNode = factory.endNode(ids.getAndIncrement(), "EmbeddedEnd", false,
                            embeddedSubProcess);
                    if (eventState.isExclusive()) {
                        // use event based gateway

                        Split eventSplit = factory.eventBasedSplit(ids.getAndIncrement(), "split_" + state.getName(),
                                embeddedSubProcess);

                        Join join = factory.joinNode(ids.getAndIncrement(), "join_" + state.getName(), Join.TYPE_XOR,
                                embeddedSubProcess);

                        factory.connect(embeddedStartNode.getId(), eventSplit.getId(),
                                "connection_" + embeddedStartNode.getId() + "_" + eventSplit.getId(), embeddedSubProcess,
                                false);

                        for (String eventRef : onEvent.getEventRefs()) {

                            EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                                    .filter(e -> e.getName().equals(eventRef)).findFirst().get();

                            EventNode eventNode = factory.consumeEventNode(ids.getAndIncrement(), event,
                                    onEvent.getEventDataFilter(),
                                    embeddedSubProcess);

                            factory.connect(eventSplit.getId(), eventNode.getId(),
                                    "connection_" + eventSplit.getId() + "_" + eventNode.getId(), embeddedSubProcess,
                                    false);

                            factory.connect(eventNode.getId(), join.getId(),
                                    "connection_" + eventNode.getId() + "_" + join.getId(), embeddedSubProcess,
                                    false);
                        }

                        buildActionsForState(workflow, onEvent.getActions(), embeddedSubProcess, factory, ids,
                                (first, last) -> {
                                    factory.connect(join.getId(), first.getId(),
                                            join.getId() + "_" + first.getId(),
                                            embeddedSubProcess, false);

                                    factory.connect(last.getId(), embeddedEndNode.getId(),
                                            last.getId() + "_" + embeddedEndNode.getId(),
                                            embeddedSubProcess, false);
                                }, (first, last) -> {
                                }, false);
                    } else {
                        // use parallel gateway

                        Split parallelSplit = factory.splitNode(ids.getAndIncrement(), "split_" + state.getName(),
                                Split.TYPE_AND,
                                embeddedSubProcess);

                        Join parallelJoin = factory.joinNode(ids.getAndIncrement(), "join_" + state.getName(), Join.TYPE_AND,
                                embeddedSubProcess);

                        factory.connect(embeddedStartNode.getId(), parallelSplit.getId(),
                                "connection_" + embeddedStartNode.getId() + "_" + parallelSplit.getId(), embeddedSubProcess,
                                false);

                        for (String eventRef : onEvent.getEventRefs()) {

                            EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                                    .filter(e -> e.getName().equals(eventRef)).findFirst().get();

                            EventNode eventNode = factory.consumeEventNode(ids.getAndIncrement(), event,
                                    onEvent.getEventDataFilter(),
                                    embeddedSubProcess);

                            factory.connect(parallelSplit.getId(), eventNode.getId(),
                                    "connection_" + parallelSplit.getId() + "_" + eventNode.getId(), embeddedSubProcess,
                                    false);

                            factory.connect(eventNode.getId(), parallelJoin.getId(),
                                    "connection_" + eventNode.getId() + "_" + parallelJoin.getId(), embeddedSubProcess,
                                    false);
                        }

                        buildActionsForState(workflow, onEvent.getActions(), embeddedSubProcess, factory, ids,
                                (first, last) -> {
                                    factory.connect(parallelJoin.getId(), first.getId(),
                                            parallelJoin.getId() + "_" + first.getId(),
                                            embeddedSubProcess, false);

                                    factory.connect(last.getId(), embeddedEndNode.getId(),
                                            last.getId() + "_" + embeddedEndNode.getId(),
                                            embeddedSubProcess, false);
                                }, (first, last) -> {
                                }, false);

                    }

                    if (state.getEnd() != null) {

                        EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                                state.getEnd().isTerminate(),
                                process);
                        if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                            produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                    embeddedSubProcess.getId(),
                                    endNode.getId());
                        } else {
                            factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                    "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                        }
                    }
                }

            } else if (state.getType().equals(DefaultState.Type.CALLBACK)) {
                CallbackState callcackState = (CallbackState) state;
                CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                        process);
                currentNode = embeddedSubProcess;

                // handle state data inputs
                Assignment inputAssignment = new Assignment("jq", "", "");
                inputAssignment.setMetaData("Action", new InputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getInput())));
                embeddedSubProcess.addInAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(inputAssignment), null));

                // handle state data outputs
                Assignment outputAssignment = new Assignment("jq", "", "");
                outputAssignment.setMetaData("Action", new OutputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getOutput())));
                embeddedSubProcess.addOutAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(outputAssignment), null));
                mappedNodes.put(state.getName(), embeddedSubProcess.getId());

                StartNode embeddedStartNode = factory.startNode(ids.getAndIncrement(), "EmbeddedStart", embeddedSubProcess);
                EndNode embeddedEndNode = factory.endNode(ids.getAndIncrement(), "EmbeddedEnd", false, embeddedSubProcess);

                EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                        .filter(e -> e.getName().equals(callcackState.getEventRef())).findFirst().get();

                EventNode eventNode = factory.consumeEventNode(ids.getAndIncrement(), event, callcackState.getEventDataFilter(),
                        embeddedSubProcess);

                buildActionsForState(workflow, Collections.singletonList(callcackState.getAction()), embeddedSubProcess,
                        factory, ids,
                        (first, last) -> {
                            factory.connect(embeddedStartNode.getId(), first.getId(),
                                    embeddedStartNode.getId() + "_" + first.getId(),
                                    embeddedSubProcess, false);

                            factory.connect(last.getId(), eventNode.getId(),
                                    last.getId() + "_" + eventNode.getId(),
                                    embeddedSubProcess, false);
                        }, (first, last) -> {
                        }, false);

                factory.connect(eventNode.getId(), embeddedEndNode.getId(),
                        "connection_" + eventNode.getId() + "_" + embeddedEndNode.getId(), embeddedSubProcess, false);

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                embeddedSubProcess.getId(),
                                endNode.getId());
                    } else {
                        factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                    }
                }

            } else if (state.getType().equals(Type.SLEEP)) {

                TimerNode sleep = factory.timerNode(ids.getAndIncrement(), "sleep-" + state.getName(),
                        ((SleepState) state).getDuration(), process);
                mappedNodes.put(state.getName(), sleep.getId());

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process, sleep.getId(),
                                endNode.getId());
                    } else {
                        factory.connect(sleep.getId(), endNode.getId(),
                                "connection_" + sleep.getId() + "_" + endNode.getId(), process, false);
                    }
                }
                currentNode = sleep;
            } else if (state.getType().equals(DefaultState.Type.PARALLEL)) {

                ParallelState parallelState = (ParallelState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                        process);

                currentNode = embeddedSubProcess;
                // handle state data inputs
                Assignment inputAssignment = new Assignment("jq", "", "");
                inputAssignment.setMetaData("Action", new InputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getInput())));
                embeddedSubProcess.addInAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(inputAssignment), null));

                // handle state data outputs
                Assignment outputAssignment = new Assignment("jq", "", "");
                outputAssignment.setMetaData("Action", new OutputJqAssignmentAction(
                        state.getStateDataFilter() == null ? null
                                : factory.unwrapExpression(state.getStateDataFilter().getOutput())));
                embeddedSubProcess.addOutAssociation(
                        new DataAssociation(Collections.emptyList(), "", Arrays.asList(outputAssignment), null));
                mappedNodes.put(state.getName(), embeddedSubProcess.getId());

                StartNode embeddedStartNode = factory.startNode(ids.getAndIncrement(), "EmbeddedStart", embeddedSubProcess);
                EndNode embeddedEndNode = factory.endNode(ids.getAndIncrement(), "EmbeddedEnd", false, embeddedSubProcess);

                Split parallelSplit = factory.splitNode(ids.getAndIncrement(), "split_" + state.getName(), Split.TYPE_AND,
                        embeddedSubProcess);

                Join parallelJoin;

                if (parallelState.getCompletionType().equals(CompletionType.AT_LEAST)) {
                    parallelJoin = factory.joinNode(ids.getAndIncrement(), "join_" + state.getName(), Join.TYPE_N_OF_M,
                            embeddedSubProcess);
                    parallelJoin.setN(parallelState.getNumCompleted());
                } else {
                    parallelJoin = factory.joinNode(ids.getAndIncrement(), "join_" + state.getName(), Join.TYPE_AND,
                            embeddedSubProcess);
                }

                factory.connect(embeddedStartNode.getId(), parallelSplit.getId(),
                        "connection_" + embeddedStartNode.getId() + "_" + parallelSplit.getId(), embeddedSubProcess, false);

                for (Branch branch : parallelState.getBranches()) {
                    buildActionsForState(workflow, branch.getActions(), embeddedSubProcess, factory, ids,
                            (first, last) -> {
                                factory.connect(parallelSplit.getId(), first.getId(),
                                        parallelSplit.getId() + "_" + first.getId(),
                                        embeddedSubProcess, false);

                                factory.connect(last.getId(), parallelJoin.getId(),
                                        last.getId() + "_" + parallelJoin.getId(),
                                        embeddedSubProcess, false);
                            }, (first, last) -> {
                            }, true);
                }
                factory.connect(parallelJoin.getId(), embeddedEndNode.getId(),
                        "connection_" + parallelJoin.getId() + "_" + embeddedEndNode.getId(), embeddedSubProcess, false);

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    if (state.getEnd().getProduceEvents() != null && !state.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(state.getEnd().getProduceEvents(), factory, workflow, ids, process,
                                embeddedSubProcess.getId(),
                                endNode.getId());
                    } else {
                        factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                    }
                }

            } else if (state.getType().equals(DefaultState.Type.FOREACH)) {

            }
            // ensure that start node is connected
            if (state.equals(start) && currentNode != null) {
                factory.connect(startNode.getId(), currentNode.getId(),
                        "connection_" + startNode.getId() + "_" + currentNode.getId(), process, false);
            }
        }

        for (State state : workflow.getStates()) {
            if (state.getType().equals(Type.SWITCH)) {
                // switch state must be processed at the end as it needs to reference other nodes by id
                SwitchState switchState = (SwitchState) state;

                if (switchState.getDataConditions() != null && !switchState.getDataConditions().isEmpty()) {

                    Split splitNode = factory.splitNode(ids.getAndIncrement(), "split_" + state.getName(), Split.TYPE_XOR,
                            process);
                    currentNode = splitNode;

                    mappedNodes.put(state.getName(), splitNode.getId());
                    int priority = 1;
                    for (DataCondition condition : switchState.getDataConditions()) {

                        boolean isDefaultConstraint = false;

                        if (switchState.getDefaultCondition() != null
                                && switchState.getDefaultCondition().getTransition() != null
                                && condition.getTransition() != null &&
                                condition.getTransition().getNextState()
                                        .equals(switchState.getDefaultCondition().getTransition().getNextState())) {
                            isDefaultConstraint = true;
                        }

                        if (switchState.getDefaultCondition() != null && switchState.getDefaultCondition().getEnd() != null
                                && condition.getEnd() != null) {
                            isDefaultConstraint = true;
                        }
                        Connection outgoingConnection = null;
                        long target = 0;
                        if (condition.getEnd() != null) {
                            EndNode endNode = factory.endNode(ids.getAndIncrement(), "end_" + switchState.getName(), false,
                                    process);
                            target = endNode.getId();

                            outgoingConnection = factory.connect(splitNode.getId(), endNode.getId(),
                                    "connection_" + splitNode.getId() + "_" + endNode.getId(), process, false);
                        } else if (condition.getTransition() != null && condition.getTransition().getNextState() != null) {
                            long source = splitNode.getId();

                            target = mappedNodes.get(condition.getTransition().getNextState());

                            outgoingConnection = factory.connect(source, target, "connection_" + source + "_" + target, process,
                                    false);

                        }

                        ReturnValueConstraintEvaluator returnValueConstraint = new ReturnValueConstraintEvaluator();
                        returnValueConstraint.setDialect("jq");
                        returnValueConstraint.setName(splitNode.getId() + "_" + target);
                        returnValueConstraint.setPriority(priority);
                        returnValueConstraint.setDefault(isDefaultConstraint);
                        returnValueConstraint.setType("DROOLS_DEFAULT");
                        returnValueConstraint.setConstraint(factory.unwrapExpression(condition.getCondition()));
                        returnValueConstraint
                                .setEvaluator(new JqReturnValueEvaluator(factory.unwrapExpression(condition.getCondition())));

                        splitNode.setConstraint(outgoingConnection, returnValueConstraint);
                    }
                } else if (switchState.getEventConditions() != null && !switchState.getEventConditions().isEmpty()) {

                    Split splitNode = factory.eventBasedSplit(ids.getAndIncrement(), "split_" + state.getName(),
                            process);
                    currentNode = splitNode;

                    mappedNodes.put(state.getName(), splitNode.getId());

                    for (EventCondition eventCondition : switchState.getEventConditions()) {

                        EventDefinition event = WorkflowUtils.getDefinedConsumedEvents(workflow).stream()
                                .filter(e -> e.getName().equals(eventCondition.getEventRef())).findFirst().get();

                        EventNode eventNode = factory.consumeEventNode(ids.getAndIncrement(), event,
                                eventCondition.getEventDataFilter(),
                                process);

                        factory.connect(splitNode.getId(), eventNode.getId(),
                                "connection_" + splitNode.getId() + "_" + eventNode.getId(), process,
                                false);

                        long target = 0;
                        if (eventCondition.getEnd() != null) {
                            EndNode endNode = factory.endNode(ids.getAndIncrement(), "end_" + switchState.getName(), false,
                                    process);
                            target = endNode.getId();

                            factory.connect(splitNode.getId(), endNode.getId(),
                                    "connection_" + splitNode.getId() + "_" + endNode.getId(), process, false);
                        } else if (eventCondition.getTransition() != null
                                && eventCondition.getTransition().getNextState() != null) {
                            target = mappedNodes.get(eventCondition.getTransition().getNextState());

                            factory.connect(eventNode.getId(), target,
                                    "connection_" + eventNode.getId() + "_" + target, process,
                                    false);
                        }
                    }

                    if (switchState.getTimeouts() != null && switchState.getTimeouts().getEventTimeout() != null) {

                        TimerNode timer = factory.timerNode(ids.getAndIncrement(), "event-switch-timeout",
                                switchState.getTimeouts().getEventTimeout(), process);

                        factory.connect(splitNode.getId(), timer.getId(),
                                "connection_" + splitNode.getId() + "_" + timer.getId(), process,
                                false);

                        EndNode endNode = factory.endNode(ids.getAndIncrement(), "end_" + switchState.getName(), false,
                                process);

                        factory.connect(timer.getId(), endNode.getId(),
                                "connection_" + timer.getId() + "_" + endNode.getId(), process, false);
                    }

                }
                // ensure that start node is connected
                if (state.equals(start) && currentNode != null) {
                    factory.connect(startNode.getId(), currentNode.getId(),
                            "connection_" + startNode.getId() + "_" + currentNode.getId(), process, false);
                }
            }

        }

        // next connect all nodes
        for (State state : workflow.getStates()) {

            if (!mappedNodes.containsKey(state.getName())) {
                continue;
            }

            long source = mappedNodes.get(state.getName());
            if (state.getTransition() != null && state.getTransition().getNextState() != null) {
                long target = mappedNodes.get(state.getTransition().getNextState());

                if (state.getTransition().getProduceEvents() != null && !state.getTransition().getProduceEvents().isEmpty()) {
                    produceEvents(state.getTransition().getProduceEvents(), factory, workflow, ids, process, source,
                            target);
                } else {

                    factory.connect(source, target, "connection_" + source + "_" + target, process, false);
                }
            }
        }
        // attach error handling
        for (State state : workflow.getStates()) {
            if (!mappedNodes.containsKey(state.getName())) {
                continue;
            }
            long source = mappedNodes.get(state.getName());
            if (process.getNode(source) instanceof CompositeContextNode) {
                addErrorHandlingToState(workflow, state, factory, ids, process, (CompositeContextNode) process.getNode(source));
            }
        }
        factory.validate((ExecutableProcess) process);

        if (workflow.getTimeouts() != null && workflow.getTimeouts().getWorkflowExecTimeout() != null) {
            factory.addExecutionTimeout(ids.getAndIncrement(), workflow.getTimeouts().getWorkflowExecTimeout(),
                    (ExecutableProcess) process);
        }

        process.setMetaData("SW-Workflow", workflow);

        return process;
    }

    public String toString(Reader reader) {
        try {
            char[] arr = new char[8 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            reader.close();
            return buffer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void buildActionsForState(Workflow workflow, List<Action> actions,
            NodeContainer embeddedSubProcess, ServerlessWorkflowFactory factory, AtomicLong ids,
            BiConsumer<Node, Node> firstLastNodeConsumer, BiConsumer<Node, Node> actionConsumer, boolean isParallel) {
        Node firstNode = null;
        Node lastNode = null;
        Node prevNode = null;
        for (Action action : actions) {

            if (action.getFunctionRef() != null) {
                // handle function based action

                Optional<FunctionDefinition> functionDefinition = workflow.getFunctions().getFunctionDefs()
                        .stream()
                        .filter(functionDef -> functionDef.getName().equals(action.getFunctionRef().getRefName()))
                        .distinct()
                        .findFirst();

                if (functionDefinition.get().getType() == FunctionDefinition.Type.EXPRESSION) {
                    ActionNode actionNode = factory.expressionActionStateNode(ids.getAndIncrement(),
                            action.getName(),
                            embeddedSubProcess,
                            functionDefinition.get().getOperation(),
                            action);

                    if (firstNode == null) {
                        firstNode = actionNode;
                    }
                    lastNode = actionNode;

                } else if (functionDefinition.get().getType() == null
                        || functionDefinition.get().getType() == FunctionDefinition.Type.REST) {
                    WorkItemNode serviceNode = factory.serviceNode(ids.getAndIncrement(), action, functionDefinition.get(),
                            embeddedSubProcess);
                    if (firstNode == null) {
                        firstNode = serviceNode;
                    }
                    lastNode = serviceNode;

                    if (workflow.getTimeouts() != null && workflow.getTimeouts().getActionExecTimeout() != null) {
                        serviceNode.setMetaData("timeout",
                                String.valueOf(DateTimeUtils.parseDuration(workflow.getTimeouts().getActionExecTimeout())));
                    }

                    if (action.getRetryableErrors() != null && !action.getRetryableErrors().isEmpty()) {

                        List<ErrorDefinition> defs = new ArrayList<>();
                        for (String errorRef : action.getRetryableErrors()) {
                            ErrorDefinition errorDef = workflow.getErrors().getErrorDefs().stream()
                                    .filter(error -> error.getName().equals(errorRef))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalStateException("Missing error definition for " + errorRef));

                            defs.add(errorDef);
                        }
                        RetryDefinition retry = null;
                        BoundaryEventNode errorNode = factory.errorBoundaryEventNode(ids.getAndIncrement(), defs, retry,
                                embeddedSubProcess, serviceNode, workflow);

                        EndNode onErrorEnd = factory.endNode(ids.getAndIncrement(), action.getName() + "onErrorEnd", false,
                                embeddedSubProcess);
                        factory.connect(errorNode.getId(), onErrorEnd.getId(),
                                "connect_" + errorNode.getId() + "_" + onErrorEnd.getId(), embeddedSubProcess, false);
                    }
                } else {
                    throw new UnsupportedOperationException(
                            functionDefinition.get().getType() + " is not yet supported");
                }

            } else if (action.getSubFlowRef() != null) {
                // handler sub workflow action definition
                String workflowId = Objects.requireNonNull(action.getSubFlowRef().getWorkflowId(),
                        "Workflow id for subworkflow is mandatory");

                boolean independent = false;
                if (action.getSubFlowRef().getOnParentComplete() != null
                        && action.getSubFlowRef().getOnParentComplete().equals(OnParentComplete.CONTINUE)) {
                    independent = true;
                }
                boolean waitForCompletion = true;
                if (action.getSubFlowRef().getInvoke().equals(Invoke.ASYNC)) {
                    waitForCompletion = false;
                }

                SubProcessNode callactivity = factory.callActivity(ids.getAndIncrement(), action.getName(), workflowId,
                        waitForCompletion,
                        embeddedSubProcess);

                callactivity.setIndependent(independent);
                callactivity.setProcessVersion(action.getSubFlowRef().getVersion());
                if (firstNode == null) {
                    firstNode = callactivity;
                }
                lastNode = callactivity;

            }
            if (action.getSleep() != null && action.getSleep().getBefore() != null) {
                TimerNode sleep = factory.timerNode(ids.getAndIncrement(), "sleep-before-" + action.getName(),
                        action.getSleep().getBefore(), embeddedSubProcess);
                factory.connect(sleep.getId(), firstNode.getId(), "connection_" + sleep.getId() + "_" + firstNode.getId(),
                        embeddedSubProcess, false);
                firstNode = sleep;
            }

            if (action.getSleep() != null && action.getSleep().getAfter() != null) {
                TimerNode sleep = factory.timerNode(ids.getAndIncrement(), "sleep-after-" + action.getName(),
                        action.getSleep().getAfter(), embeddedSubProcess);

                factory.connect(lastNode.getId(), sleep.getId(), "connection_" + lastNode.getId() + "_" + sleep.getId(),
                        embeddedSubProcess, false);
                lastNode = sleep;
            }
            actionConsumer.accept(firstNode, lastNode);

            if (isParallel && actions.size() > 1) {
                // reset first node as all of action nodes will be first nodes
                firstNode = null;
            } else {
                if (prevNode != null) {
                    factory.connect(prevNode.getId(), lastNode.getId(),
                            "connect_" + prevNode.getId() + "_" + lastNode.getId(), embeddedSubProcess, false);
                }
            }

            prevNode = lastNode;
        }
        firstLastNodeConsumer.accept(firstNode, lastNode);
    }

    protected void addErrorHandlingToState(Workflow workflow, State state, ServerlessWorkflowFactory factory, AtomicLong ids,
            WorkflowProcess process, CompositeContextNode subprocess) {

        if (state.getOnErrors() != null) {

            for (io.serverlessworkflow.api.error.Error error : state.getOnErrors()) {

                List<ErrorDefinition> defs = new ArrayList<>();
                if (error.getErrorRef() != null) {
                    workflow.getErrors().getErrorDefs().stream().filter(err -> err.getName().equals(error.getErrorRef()))
                            .forEach(err -> defs.add(err));
                } else {
                    workflow.getErrors().getErrorDefs().stream().filter(err -> error.getErrorRefs().contains(err.getName()))
                            .forEach(err -> defs.add(err));
                }

                BoundaryEventNode errorNode = factory.errorBoundaryEventNode(ids.getAndIncrement(), defs, null,
                        process, subprocess, workflow);

                if (error.getEnd() != null) {
                    EndNode onErrorEnd = factory.endNode(ids.getAndIncrement(), state.getName() + "onErrorEnd",
                            error.getEnd().isTerminate(), process);

                    if (error.getEnd().getProduceEvents() != null && !error.getEnd().getProduceEvents().isEmpty()) {
                        produceEvents(error.getEnd().getProduceEvents(), factory, workflow, ids, process, errorNode.getId(),
                                onErrorEnd.getId());
                    } else {

                        factory.connect(errorNode.getId(), onErrorEnd.getId(),
                                "connect_" + errorNode.getId() + "_" + onErrorEnd.getId(), process, false);
                    }
                } else {

                    if (error.getTransition().getNextState() != null) {

                        for (io.automatiko.engine.api.definition.process.Node node : process.getNodes()) {
                            if (node.getName().equals(error.getTransition().getNextState())) {

                                if (error.getTransition().getProduceEvents() != null
                                        && !error.getTransition().getProduceEvents().isEmpty()) {
                                    produceEvents(error.getTransition().getProduceEvents(), factory, workflow, ids, process,
                                            errorNode.getId(), node.getId());
                                } else {

                                    factory.connect(errorNode.getId(), node.getId(),
                                            "connect_" + errorNode.getId() + "_" + node.getId(), process, false);
                                }
                                break;
                            }
                        }

                    }
                }

            }

        }
    }

    protected void produceEvents(List<ProduceEvent> events, ServerlessWorkflowFactory factory, Workflow workflow,
            AtomicLong ids,
            NodeContainer container, long startNodeId, long endNodeId) {
        Node prevNode = null;
        Node currentNode = null;

        for (ProduceEvent event : events) {

            currentNode = factory.produceMessageNode(ids.getAndIncrement(), "produceEvent_" + startNodeId, workflow, event,
                    container);

            if (prevNode != null) {
                factory.connect(prevNode.getId(), currentNode.getId(),
                        "connect_" + prevNode.getId() + "_" + currentNode.getId(), container, false);
            } else {
                factory.connect(startNodeId, currentNode.getId(),
                        "connect_" + startNodeId + "_" + currentNode.getId(), container, false);
            }
            prevNode = currentNode;
        }

        factory.connect(prevNode.getId(), endNodeId,
                "connect_" + prevNode.getId() + "_" + endNodeId, container, false);
    }
}
