package io.automatiko.engine.workflow.serverless;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.instance.impl.jq.InputJqAssignmentAction;
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
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.DefaultState.Type;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.OperationState.ActionMode;
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

        // process all states and create proper node representation for each state
        for (State state : workflow.getStates()) {

            Node currentNode = null;

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

                if (operationState.getActionMode() == null || operationState.getActionMode() == ActionMode.SEQUENTIAL) {

                    buildActionsForOperationState(workflow, operationState, embeddedSubProcess, factory, ids, (first, last) -> {
                        factory.connect(embeddedStartNode.getId(), first.getId(),
                                embeddedStartNode.getId() + "_" + first.getId(),
                                embeddedSubProcess, false);

                        factory.connect(last.getId(), embeddedEndNode.getId(), last.getId() + "_" + embeddedEndNode.getId(),
                                embeddedSubProcess, false);
                    }, actionNode -> {
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

                    buildActionsForOperationState(workflow, operationState, embeddedSubProcess, factory, ids, (first, last) -> {

                    }, actionNode -> {
                        factory.connect(split.getId(), actionNode.getId(),
                                split.getId() + "_" + actionNode.getId(),
                                embeddedSubProcess, false);

                        factory.connect(actionNode.getId(), join.getId(), actionNode.getId() + "_" + join.getId(),
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

            }
            // ensure that start node is connected
            if (state.equals(start) && currentNode != null) {
                factory.connect(startNode.getId(), currentNode.getId(),
                        "connection_" + startNode.getId() + "_" + currentNode.getId(), process, false);
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

    protected void buildActionsForOperationState(Workflow workflow, OperationState operationState,
            NodeContainer embeddedSubProcess, ServerlessWorkflowFactory factory, AtomicLong ids,
            BiConsumer<Node, Node> firstLastNodeConsumer, Consumer<Node> actionConsumer) {
        Node firstNode = null;
        Node lastNode = null;
        for (Action action : operationState.getActions()) {

            Optional<FunctionDefinition> functionDefinition = workflow.getFunctions().getFunctionDefs()
                    .stream()
                    .filter(functionDef -> functionDef.getName().equals(action.getFunctionRef().getRefName()))
                    .distinct()
                    .findFirst();

            if (functionDefinition.isPresent()) {
                if (functionDefinition.get().getType() == FunctionDefinition.Type.EXPRESSION) {
                    ActionNode actionNode = factory.expressionActionStateNode(ids.getAndIncrement(),
                            action.getName(),
                            embeddedSubProcess,
                            functionDefinition.get().getOperation(),
                            action.getActionDataFilter());

                    if (firstNode == null) {
                        firstNode = actionNode;
                    }
                    lastNode = actionNode;

                    actionConsumer.accept(actionNode);
                } else if (functionDefinition.get().getType() == null
                        || functionDefinition.get().getType() == FunctionDefinition.Type.REST) {
                    WorkItemNode serviceNode = factory.serviceNode(ids.getAndIncrement(), action, functionDefinition.get(),
                            embeddedSubProcess);
                    if (firstNode == null) {
                        firstNode = serviceNode;
                    }
                    lastNode = serviceNode;

                    actionConsumer.accept(serviceNode);
                } else {
                    throw new UnsupportedOperationException(
                            functionDefinition.get().getType() + " is not yet supported");
                }
            }
        }
        firstLastNodeConsumer.accept(firstNode, lastNode);
    }
}
