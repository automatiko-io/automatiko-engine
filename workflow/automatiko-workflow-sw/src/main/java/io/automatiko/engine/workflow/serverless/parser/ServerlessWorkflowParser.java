package io.automatiko.engine.workflow.serverless.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
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
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.functions.SubFlowRef.Invoke;
import io.serverlessworkflow.api.functions.SubFlowRef.OnParentComplete;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.DefaultState.Type;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.OperationState.ActionMode;
import io.serverlessworkflow.api.states.ParallelState;
import io.serverlessworkflow.api.states.ParallelState.CompletionType;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.utils.WorkflowUtils;

public class ServerlessWorkflowParser {

    public Process parse(Reader workflowFile) {
        WorkflowProcess process = null;
        AtomicLong ids = new AtomicLong(0);
        Workflow workflow = Workflow.fromSource(toString(workflowFile));

        ServerlessWorkflowFactory factory = new ServerlessWorkflowFactory();
        if (!"jq".equalsIgnoreCase(workflow.getExpressionLang())) {
            throw new IllegalArgumentException("Not supported expression language, only 'jq' is supported");
        }
        process = factory.createProcess(workflow);

        State start = WorkflowUtils.getStartingState(workflow);
        StartNode startNode = factory.startNode(ids.getAndIncrement(), start.getName() + "-start", process);

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
                    factory.connect(actionNode.getId(), endNode.getId(),
                            "connection_" + actionNode.getId() + "_" + endNode.getId(), process, false);
                }
                setUniqueId(actionNode, state);
                currentNode = actionNode;
            } else if (state.getType().equals(DefaultState.Type.OPERATION)) {
                OperationState operationState = (OperationState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                        process);
                currentNode = embeddedSubProcess;
                setUniqueId(embeddedSubProcess, state);
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
                        factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                                "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
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
                            });

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
                            });
                }

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                            "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
                }
            } else if (state.getType().equals(DefaultState.Type.CALLBACK)) {

            } else if (state.getType().equals(Type.SLEEP)) {

                TimerNode sleep = factory.timerNode(ids.getAndIncrement(), "sleep-" + state.getName(),
                        ((SleepState) state).getDuration(), process);
                mappedNodes.put(state.getName(), sleep.getId());
                setUniqueId(sleep, state);
                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    factory.connect(sleep.getId(), endNode.getId(),
                            "connection_" + sleep.getId() + "_" + endNode.getId(), process, false);
                }
                currentNode = sleep;
            } else if (state.getType().equals(DefaultState.Type.PARALLEL)) {

                ParallelState parallelState = (ParallelState) state;

                CompositeContextNode embeddedSubProcess = factory.subProcessNode(ids.getAndIncrement(), state.getName(),
                        process);
                setUniqueId(embeddedSubProcess, state);
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
                            });
                }
                factory.connect(parallelJoin.getId(), embeddedEndNode.getId(),
                        "connection_" + parallelJoin.getId() + "_" + embeddedEndNode.getId(), embeddedSubProcess, false);

                if (state.getEnd() != null) {

                    EndNode endNode = factory.endNode(ids.getAndIncrement(), state.getName() + "-end",
                            state.getEnd().isTerminate(),
                            process);
                    factory.connect(embeddedSubProcess.getId(), endNode.getId(),
                            "connection_" + embeddedSubProcess.getId() + "_" + endNode.getId(), process, false);
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
                    setUniqueId(splitNode, state);
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
                }
                // ensure that start node is connected
                if (state.equals(start) && currentNode != null) {
                    factory.connect(startNode.getId(), currentNode.getId(),
                            "connection_" + startNode.getId() + "_" + currentNode.getId(), process, false);
                }
            }

        }

        // lastly connect all nodes
        for (State state : workflow.getStates()) {

            long source = mappedNodes.get(state.getName());
            if (state.getTransition() != null && state.getTransition().getNextState() != null) {
                long target = mappedNodes.get(state.getTransition().getNextState());

                factory.connect(source, target, "connection_" + source + "_" + target, process, false);
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
            BiConsumer<Node, Node> firstLastNodeConsumer, BiConsumer<Node, Node> actionConsumer) {
        Node firstNode = null;
        Node lastNode = null;
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
                    if (action.getSleep() == null
                            || (action.getSleep().getBefore() == null && action.getSleep().getAfter() == null)) {
                        actionConsumer.accept(actionNode, actionNode);
                    }
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

                    if (action.getSleep() == null
                            || (action.getSleep().getBefore() == null && action.getSleep().getAfter() == null)) {
                        actionConsumer.accept(serviceNode, serviceNode);
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
                if (action.getSleep() == null
                        || (action.getSleep().getBefore() == null && action.getSleep().getAfter() == null)) {
                    actionConsumer.accept(callactivity, callactivity);
                }

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
        }
        firstLastNodeConsumer.accept(firstNode, lastNode);
    }

    protected void setUniqueId(Node node, State state) {
        node.setMetaData("UniqueId", UUID.nameUUIDFromBytes(state.getName().getBytes(StandardCharsets.UTF_8)).toString());
    }
}
