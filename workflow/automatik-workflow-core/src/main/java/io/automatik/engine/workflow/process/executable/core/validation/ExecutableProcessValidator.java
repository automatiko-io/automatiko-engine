
package io.automatik.engine.workflow.process.executable.core.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.mvel2.ErrorDetail;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExpressionCompiler;

import io.automatik.engine.api.definition.process.Connection;
import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.NodeContainer;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.io.Resource;
import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatik.engine.workflow.base.core.validation.ProcessValidator;
import io.automatik.engine.workflow.base.core.validation.impl.ProcessValidationErrorImpl;
import io.automatik.engine.workflow.process.core.WorkflowProcess;
import io.automatik.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.ActionNode;
import io.automatik.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatik.engine.workflow.process.core.node.CatchLinkNode;
import io.automatik.engine.workflow.process.core.node.CompositeNode;
import io.automatik.engine.workflow.process.core.node.CompositeNode.CompositeNodeEnd;
import io.automatik.engine.workflow.process.core.node.CompositeNode.NodeAndType;
import io.automatik.engine.workflow.process.core.node.DynamicNode;
import io.automatik.engine.workflow.process.core.node.EndNode;
import io.automatik.engine.workflow.process.core.node.EventNode;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatik.engine.workflow.process.core.node.FaultNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode.ForEachJoinNode;
import io.automatik.engine.workflow.process.core.node.ForEachNode.ForEachSplitNode;
import io.automatik.engine.workflow.process.core.node.Join;
import io.automatik.engine.workflow.process.core.node.MilestoneNode;
import io.automatik.engine.workflow.process.core.node.RuleSetNode;
import io.automatik.engine.workflow.process.core.node.Split;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.StateNode;
import io.automatik.engine.workflow.process.core.node.SubProcessNode;
import io.automatik.engine.workflow.process.core.node.ThrowLinkNode;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;

/**
 * Default implementation of a RuleFlow validator.
 */
public class ExecutableProcessValidator implements ProcessValidator {

    public static final String ASSOCIATIONS = "BPMN.Associations";

    private static ExecutableProcessValidator instance;

    private ExecutableProcessValidator() {
    }

    public static ExecutableProcessValidator getInstance() {
        if (instance == null) {
            instance = new ExecutableProcessValidator();
        }
        return instance;
    }

    public ProcessValidationError[] validateProcess(final ExecutableProcess process) {
        final List<ProcessValidationError> errors = new ArrayList<>();

        if (process.getName() == null) {
            errors.add(new ProcessValidationErrorImpl(process, "Process has no name."));
        }

        if (process.getId() == null || "".equals(process.getId())) {
            errors.add(new ProcessValidationErrorImpl(process, "Process has no id."));
        }

        // check start node of process
        if (process.getStartNodes().isEmpty() && !process.isDynamic()) {
            errors.add(new ProcessValidationErrorImpl(process, "Process has no start node."));
        }

        // Check end node of the process.
        if (process.getEndNodes().isEmpty() && !process.isDynamic()) {
            errors.add(new ProcessValidationErrorImpl(process, "Process has no end node."));
        }

        validateNodes(process.getNodes(), errors, process);

        validateVariables(errors, process);

        checkAllNodesConnectedToStart(process, process.isDynamic(), errors, process);

        return errors.toArray(new ProcessValidationError[errors.size()]);
    }

    private void validateNodes(Node[] nodes, List<ProcessValidationError> errors, ExecutableProcess process) {
        String isForCompensation = "isForCompensation";
        for (int i = 0; i < nodes.length; i++) {
            final Node node = nodes[i];
            if (node instanceof StartNode) {
                final StartNode startNode = (StartNode) node;
                if (startNode.getTo() == null) {
                    addErrorMessage(process, node, errors, "Start has no outgoing connection.");
                }
                if (startNode.getTimer() != null) {
                    validateTimer(startNode.getTimer(), node, process, errors);
                }
            } else if (node instanceof EndNode) {
                final EndNode endNode = (EndNode) node;
                if (endNode.getFrom() == null) {
                    addErrorMessage(process, node, errors, "End has no incoming connection.");
                }
                validateCompensationIntermediateOrEndEvent(endNode, process, errors);
            } else if (node instanceof RuleSetNode) {
                final RuleSetNode ruleSetNode = (RuleSetNode) node;
                if (ruleSetNode.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "RuleSet has no incoming connection.");
                }
                if (ruleSetNode.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    addErrorMessage(process, node, errors, "RuleSet has no outgoing connection.");
                }
                final String language = ruleSetNode.getLanguage();

                RuleSetNode.RuleType ruleType = ruleSetNode.getRuleType();
                if (RuleSetNode.DMN_LANG.equals(language) || RuleSetNode.CMMN_DMN_LANG.equals(language)) {
                    RuleSetNode.RuleType.Decision decision = (RuleSetNode.RuleType.Decision) ruleType;
                    final String namespace = decision.getNamespace();
                    if (namespace == null || "".equals(namespace)) {
                        addErrorMessage(process, node, errors, "RuleSet (DMN) has no namespace.");
                    }
                    final String model = decision.getModel();
                    if (model == null || "".equals(model)) {
                        addErrorMessage(process, node, errors, "RuleSet (DMN) has no model.");
                    }
                } else {
                    addErrorMessage(process, node, errors, "Unsupported rule language '" + language + "'");
                }
                if (ruleSetNode.getTimers() != null) {
                    for (Timer timer : ruleSetNode.getTimers().keySet()) {
                        validateTimer(timer, node, process, errors);
                    }
                }
            } else if (node instanceof Split) {
                final Split split = (Split) node;
                if (split.getType() == Split.TYPE_UNDEFINED) {
                    addErrorMessage(process, node, errors, "Split has no type.");
                }
                if (split.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Split has no incoming connection.");
                }
                if (split.getDefaultOutgoingConnections().size() < 2) {
                    addErrorMessage(process, node, errors, "Split does not have more than one outgoing connection: "
                            + split.getOutgoingConnections().size() + ".");
                }
                if (split.getType() == Split.TYPE_XOR || split.getType() == Split.TYPE_OR) {
                    for (final Iterator<Connection> it = split.getDefaultOutgoingConnections().iterator(); it
                            .hasNext();) {
                        final Connection connection = it.next();
                        if (split.getConstraint(connection) == null && !split.isDefault(connection) || (!split
                                .isDefault(connection)
                                && (split.getConstraint(connection).getConstraint() == null
                                        || split.getConstraint(connection).getConstraint().trim().length() == 0))) {
                            addErrorMessage(process, node, errors,
                                    "Split does not have a constraint for " + connection.toString() + ".");
                        }
                    }
                }
            } else if (node instanceof Join) {
                final Join join = (Join) node;
                if (join.getType() == Join.TYPE_UNDEFINED) {
                    addErrorMessage(process, node, errors, "Join has no type.");
                }
                if (join.getDefaultIncomingConnections().size() < 2) {
                    addErrorMessage(process, node, errors, "Join does not have more than one incoming connection: "
                            + join.getIncomingConnections().size() + ".");
                }
                if (join.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    addErrorMessage(process, node, errors, "Join has no outgoing connection.");
                }
                if (join.getType() == Join.TYPE_N_OF_M) {
                    String n = join.getN();
                    if (!n.startsWith("#{") || !n.endsWith("}")) {
                        try {
                            Integer.parseInt(n);
                        } catch (NumberFormatException e) {
                            addErrorMessage(process, node, errors, "Join has illegal n value: " + n);
                        }
                    }
                }
            } else if (node instanceof MilestoneNode) {
                final MilestoneNode milestone = (MilestoneNode) node;
                if (milestone.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Milestone has no incoming connection.");
                }

                if (milestone.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    addErrorMessage(process, node, errors, "Milestone has no outgoing connection.");
                }
                if (milestone.getTimers() != null) {
                    for (Timer timer : milestone.getTimers().keySet()) {
                        validateTimer(timer, node, process, errors);
                    }
                }
            } else if (node instanceof StateNode) {
                final StateNode stateNode = (StateNode) node;
                if (stateNode.getDefaultIncomingConnections().isEmpty() && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "State has no incoming connection");
                }
            } else if (node instanceof SubProcessNode) {
                final SubProcessNode subProcess = (SubProcessNode) node;
                if (subProcess.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "SubProcess has no incoming connection.");
                }
                if (subProcess.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    Object compensationObj = subProcess.getMetaData(isForCompensation);
                    if (compensationObj == null || !((Boolean) compensationObj)) {
                        addErrorMessage(process, node, errors, "SubProcess has no outgoing connection.");
                    }
                }
                if (subProcess.getProcessId() == null && subProcess.getProcessName() == null) {
                    addErrorMessage(process, node, errors, "SubProcess has no process id.");
                }
                if (subProcess.getTimers() != null) {
                    for (Timer timer : subProcess.getTimers().keySet()) {
                        validateTimer(timer, node, process, errors);
                    }
                }
                if (!subProcess.isIndependent() && !subProcess.isWaitForCompletion()) {
                    addErrorMessage(process, node, errors, "SubProcess you can only set "
                            + "independent to 'false' only when 'Wait for completion' is set to true.");
                }
            } else if (node instanceof ActionNode) {
                final ActionNode actionNode = (ActionNode) node;
                if (actionNode.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Action has no incoming connection.");
                }
                if (actionNode.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    Object compensationObj = actionNode.getMetaData(isForCompensation);
                    if (compensationObj == null || !((Boolean) compensationObj)) {
                        addErrorMessage(process, node, errors, "Action has no outgoing connection.");
                    }
                }
                // don't add message if action node action is null
                // with codegen the ActionNodeVisitor will add the action
                // so when testing outside codegen having no action
                // does not mean the action node has an error (this was true before with jBPM
                // but not in Kogito)
                if (actionNode.getAction() instanceof ConsequenceAction) {
                    ConsequenceAction processAction = (ConsequenceAction) actionNode.getAction();
                    String actionString = processAction.getConsequence();
                    if (actionString == null) {
                        addErrorMessage(process, node, errors, "Action has empty action.");
                    } else if ("mvel".equals(processAction.getDialect())) {
                        try {
                            ParserContext parserContext = new ParserContext();
                            ExpressionCompiler compiler = new ExpressionCompiler(actionString, parserContext);
                            compiler.setVerifying(true);
                            compiler.compile();
                            List<ErrorDetail> mvelErrors = parserContext.getErrorList();
                            if (mvelErrors != null) {
                                for (Iterator<ErrorDetail> iterator = mvelErrors.iterator(); iterator.hasNext();) {
                                    ErrorDetail error = iterator.next();
                                    addErrorMessage(process, node, errors,
                                            "Action has invalid action: " + error.getMessage() + ".");
                                }
                            }
                        } catch (Throwable t) {
                            addErrorMessage(process, node, errors,
                                    "Action has invalid action: " + t.getMessage() + ".");
                        }
                    }
                    validateCompensationIntermediateOrEndEvent(actionNode, process, errors);
                }
            } else if (node instanceof WorkItemNode) {
                final WorkItemNode workItemNode = (WorkItemNode) node;
                if (workItemNode.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Task has no incoming connection.");
                }
                if (workItemNode.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    Object compensationObj = workItemNode.getMetaData(isForCompensation);
                    if (compensationObj == null || !((Boolean) compensationObj)) {
                        addErrorMessage(process, node, errors, "Task has no outgoing connection.");
                    }
                }
                if (workItemNode.getWork() == null) {
                    addErrorMessage(process, node, errors, "Task has no work specified.");
                } else {
                    Work work = workItemNode.getWork();
                    if (work.getName() == null || work.getName().trim().length() == 0) {
                        addErrorMessage(process, node, errors, "Task has no task type.");
                    }
                }
                if (workItemNode.getTimers() != null) {
                    for (Timer timer : workItemNode.getTimers().keySet()) {
                        validateTimer(timer, node, process, errors);
                    }
                }
            } else if (node instanceof ForEachNode) {
                final ForEachNode forEachNode = (ForEachNode) node;
                String variableName = forEachNode.getVariableName();
                if (variableName == null || "".equals(variableName)) {
                    addErrorMessage(process, node, errors, "ForEach has no variable name");
                }
                String collectionExpression = forEachNode.getCollectionExpression();
                if (collectionExpression == null || "".equals(collectionExpression)) {
                    addErrorMessage(process, node, errors, "ForEach has no collection expression");
                }
                if (forEachNode.getDefaultIncomingConnections().isEmpty() && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "ForEach has no incoming connection");
                }
                if (forEachNode.getDefaultOutgoingConnections().isEmpty() && !acceptsNoOutgoingConnections(node)) {
                    addErrorMessage(process, node, errors, "ForEach has no outgoing connection");
                }

                final List<Node> start = ExecutableProcess.getStartNodes(forEachNode.getNodes());
                if (start != null) {
                    for (Node s : start) {
                        if (((StartNode) s).getTriggers() != null && !((StartNode) s).getTriggers().isEmpty()
                                || ((StartNode) s).getTimer() != null) {
                            addErrorMessage(process, node, errors,
                                    "MultiInstance subprocess can only have none start event.");
                        }
                    }
                }
                validateNodes(forEachNode.getNodes(), errors, process);
            } else if (node instanceof DynamicNode) {
                final DynamicNode dynamicNode = (DynamicNode) node;

                if (dynamicNode.getDefaultIncomingConnections().isEmpty()
                        && !acceptsNoIncomingConnections(dynamicNode)) {
                    addErrorMessage(process, node, errors, "Dynamic has no incoming connection");
                }

                if (dynamicNode.getDefaultOutgoingConnections().isEmpty()
                        && !acceptsNoOutgoingConnections(dynamicNode)) {
                    addErrorMessage(process, node, errors, "Dynamic has no outgoing connection");
                }

                if (!dynamicNode.hasCompletionCondition() && !dynamicNode.isAutoComplete()) {
                    addErrorMessage(process, node, errors, "Dynamic has no completion condition set");
                }
                validateNodes(dynamicNode.getNodes(), errors, process);
            } else if (node instanceof CompositeNode) {
                final CompositeNode compositeNode = (CompositeNode) node;
                for (Map.Entry<String, NodeAndType> inType : compositeNode.getLinkedIncomingNodes().entrySet()) {
                    if (compositeNode.getIncomingConnections(inType.getKey()).isEmpty()
                            && !acceptsNoIncomingConnections(node)) {
                        addErrorMessage(process, node, errors,
                                "Composite has no incoming connection for type " + inType.getKey());
                    }
                    if (inType.getValue().getNode() == null && !acceptsNoOutgoingConnections(node)) {
                        addErrorMessage(process, node, errors,
                                "Composite has invalid linked incoming node for type " + inType.getKey());
                    }
                }
                for (Map.Entry<String, NodeAndType> outType : compositeNode.getLinkedOutgoingNodes().entrySet()) {
                    if (compositeNode.getOutgoingConnections(outType.getKey()).isEmpty()) {
                        addErrorMessage(process, node, errors,
                                "Composite has no outgoing connection for type " + outType.getKey());
                    }
                    if (outType.getValue().getNode() == null) {
                        addErrorMessage(process, node, errors,
                                "Composite has invalid linked outgoing node for type " + outType.getKey());
                    }
                }

                if (compositeNode.getLinkedIncomingNodes().values().isEmpty()) {
                    boolean foundStartNode = false;

                    for (Node internalNode : compositeNode.getNodes()) {
                        if (internalNode instanceof StartNode) {
                            foundStartNode = true;
                        }
                    }

                    if (!foundStartNode) {
                        addErrorMessage(process, node, errors, "Composite has no start node defined.");
                    }
                }

                if (compositeNode instanceof EventSubProcessNode) {
                    if (compositeNode.getIncomingConnections().size() > 0) {
                        addErrorMessage(process, node, errors,
                                "Event subprocess is not allowed to have any incoming connections.");
                    }
                    if (compositeNode.getOutgoingConnections().size() > 0) {
                        addErrorMessage(process, node, errors,
                                "Event subprocess is not allowed to have any outgoing connections.");
                    }
                    Node[] eventSubProcessNodes = compositeNode.getNodes();
                    int startEventCount = 0;
                    for (int j = 0; j < eventSubProcessNodes.length; ++j) {
                        if (eventSubProcessNodes[j] instanceof StartNode) {
                            StartNode startNode = (StartNode) eventSubProcessNodes[j];
                            if (++startEventCount == 2) {
                                addErrorMessage(process, compositeNode, errors,
                                        "Event subprocess is not allowed to have more than one start node.");
                            }
                            if (startNode.getTimer() == null
                                    && (startNode.getTriggers() == null || startNode.getTriggers().isEmpty())) {
                                addErrorMessage(process, startNode, errors,
                                        "Start in Event SubProcess '" + compositeNode.getName() + "' ["
                                                + compositeNode.getId()
                                                + "] must contain a trigger (event definition).");
                            }
                        }
                    }
                } else {
                    Boolean isForCompensationObject = (Boolean) compositeNode.getMetaData("isForCompensation");
                    if (compositeNode.getIncomingConnections().size() == 0
                            && !Boolean.TRUE.equals(isForCompensationObject)) {
                        addErrorMessage(process, node, errors,
                                "Embedded subprocess does not have incoming connection.");
                    }
                    if (compositeNode.getOutgoingConnections().size() == 0
                            && !Boolean.TRUE.equals(isForCompensationObject)) {
                        addErrorMessage(process, node, errors,
                                "Embedded subprocess does not have outgoing connection.");
                    }

                    final List<Node> start = ExecutableProcess.getStartNodes(compositeNode.getNodes());
                    if (start != null) {
                        for (Node s : start) {
                            if (((StartNode) s).getTriggers() != null && !((StartNode) s).getTriggers().isEmpty()
                                    || ((StartNode) s).getTimer() != null) {
                                addErrorMessage(process, node, errors,
                                        "Embedded subprocess can only have none start event.");
                            }
                        }
                    }
                }

                if (compositeNode.getTimers() != null) {
                    for (Timer timer : compositeNode.getTimers().keySet()) {
                        validateTimer(timer, node, process, errors);
                    }
                }
                validateNodes(compositeNode.getNodes(), errors, process);
            } else if (node instanceof EventNode) {
                final EventNode eventNode = (EventNode) node;
                if (eventNode.getEventFilters().isEmpty()) {
                    addErrorMessage(process, node, errors, "Event should specify an event type");
                }
                if (eventNode.getDefaultOutgoingConnections().isEmpty()) {
                    addErrorMessage(process, node, errors, "Event has no outgoing connection");
                } else {
                    List<EventFilter> eventFilters = eventNode.getEventFilters();
                    boolean compensationHandler = false;
                    for (EventFilter eventFilter : eventFilters) {
                        if (((EventTypeFilter) eventFilter).getType().startsWith("Compensation")) {
                            compensationHandler = true;
                            break;
                        }
                    }
                    if (compensationHandler && eventNode instanceof BoundaryEventNode) {
                        Connection connection = eventNode.getDefaultOutgoingConnections().get(0);
                        Boolean isAssociation = (Boolean) connection.getMetaData().get("association");
                        if (isAssociation == null) {
                            isAssociation = false;
                        }
                        if (!(eventNode.getDefaultOutgoingConnections().size() == 1 && connection != null
                                && isAssociation)) {
                            addErrorMessage(process, node, errors,
                                    "Compensation Boundary Event is only allowed to have 1 association to 1 compensation activity.");
                        }
                    }
                }
            } else if (node instanceof FaultNode) {
                final FaultNode faultNode = (FaultNode) node;
                if (faultNode.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Fault has no incoming connection.");
                }
                if (faultNode.getFaultName() == null) {
                    addErrorMessage(process, node, errors, "Fault has no fault name.");
                }
            } else if (node instanceof TimerNode) {
                TimerNode timerNode = (TimerNode) node;
                if (timerNode.getFrom() == null && !acceptsNoIncomingConnections(node)) {
                    addErrorMessage(process, node, errors, "Timer has no incoming connection.");
                }
                if (timerNode.getTo() == null && !acceptsNoOutgoingConnections(node)) {
                    addErrorMessage(process, node, errors, "Timer has no outgoing connection.");
                }
                if (timerNode.getTimer() == null) {
                    addErrorMessage(process, node, errors, "Timer has no timer specified.");
                } else {
                    validateTimer(timerNode.getTimer(), node, process, errors);
                }
            } else if (node instanceof CatchLinkNode) {
                // catchlink validation here, there also are validations in
                // ProcessHandler regarding connection issues
            } else if (node instanceof ThrowLinkNode) {
                // throw validation here, there also are validations in
                // ProcessHandler regarding connection issues
            } else {
                errors.add(new ProcessValidationErrorImpl(process,
                        "Unknown node type '" + node.getClass().getName() + "'"));
            }
        }
    }

    private void checkAllNodesConnectedToStart(final NodeContainer container, boolean isDynamic,
            final List<ProcessValidationError> errors, ExecutableProcess process) {
        final Map<Node, Boolean> processNodes = new HashMap<>();
        final Node[] nodes;
        if (container instanceof CompositeNode) {
            nodes = ((CompositeNode) container).internalGetNodes();
        } else {
            nodes = container.getNodes();
        }
        List<Node> eventNodes = new ArrayList<>();
        List<CompositeNode> compositeNodes = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            final Node node = nodes[i];
            processNodes.put(node, Boolean.FALSE);
            if (node instanceof EventNode) {
                eventNodes.add(node);
            }
            if (node instanceof CompositeNode) {
                compositeNodes.add((CompositeNode) node);
            }
        }
        if (isDynamic) {
            for (Node node : nodes) {
                if (node.getIncomingConnections(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE)
                        .isEmpty()) {
                    processNode(node, processNodes);
                }
            }
        } else {
            final List<Node> start = ExecutableProcess.getStartNodes(nodes);
            if (start != null) {
                for (Node s : start) {
                    processNode(s, processNodes);
                }
            }
            if (container instanceof CompositeNode) {
                for (CompositeNode.NodeAndType nodeAndTypes : ((CompositeNode) container).getLinkedIncomingNodes()
                        .values()) {
                    processNode(nodeAndTypes.getNode(), processNodes);
                }
            }
        }
        for (Node eventNode : eventNodes) {
            processNode(eventNode, processNodes);
        }
        for (CompositeNode compositeNode : compositeNodes) {
            checkAllNodesConnectedToStart(compositeNode, compositeNode instanceof DynamicNode, errors, process);
        }
        for (final Iterator<Node> it = processNodes.keySet().iterator(); it.hasNext();) {
            final Node node = it.next();
            if (Boolean.FALSE.equals(processNodes.get(node)) && !(node instanceof StartNode)
                    && !(node instanceof EventSubProcessNode)) {
                addErrorMessage(process, node, errors, "Has no connection to the start node.");
            }
        }
    }

    private void processNode(final Node node, final Map<Node, Boolean> nodes) {
        if (!nodes.containsKey(node) && !((node instanceof CompositeNodeEnd) || (node instanceof ForEachSplitNode)
                || (node instanceof ForEachJoinNode))) {
            throw new IllegalStateException(
                    "A process node is connected with a node that does not belong to the process: " + node.getName());
        }
        final Boolean prevValue = nodes.put(node, Boolean.TRUE);
        if (prevValue == null || Boolean.FALSE.equals(prevValue)) {
            for (final List<Connection> list : node.getOutgoingConnections().values()) {
                for (final Connection connection : list) {
                    processNode(connection.getTo(), nodes);
                }
            }
        }
    }

    private boolean acceptsNoIncomingConnections(Node node) {
        return acceptsNoOutgoingConnections(node);
    }

    private boolean acceptsNoOutgoingConnections(Node node) {
        NodeContainer nodeContainer = node.getParentContainer();
        return nodeContainer instanceof DynamicNode
                || (nodeContainer instanceof WorkflowProcess && ((WorkflowProcess) nodeContainer).isDynamic());
    }

    private void validateTimer(final Timer timer, final Node node, final ExecutableProcess process,
            final List<ProcessValidationError> errors) {
        if (timer.getDelay() == null && timer.getDate() == null) {
            addErrorMessage(process, node, errors, "Has timer with no delay or date specified.");
        } else {
            if (timer.getDelay() != null && !timer.getDelay().contains("#{")) {
                try {
                    switch (timer.getTimeType()) {
                        case Timer.TIME_CYCLE:
                            // when using ISO date/time period is not set
                            DateTimeUtils.parseRepeatableDateTime(timer.getDelay());

                            break;
                        case Timer.TIME_DURATION:
                            DateTimeUtils.parseDuration(timer.getDelay());
                            break;
                        case Timer.TIME_DATE:
                            DateTimeUtils.parseDateAsDuration(timer.getDate());
                            break;
                        default:
                            break;
                    }
                } catch (RuntimeException e) {
                    addErrorMessage(process, node, errors,
                            "Could not parse delay '" + timer.getDelay() + "': " + e.getMessage());
                }
            }
        }
        if (timer.getPeriod() != null && !timer.getPeriod().contains("#{")) {
            try {
                // when using ISO date/time period is not set
                DateTimeUtils.parseRepeatableDateTime(timer.getPeriod());

            } catch (RuntimeException e) {
                addErrorMessage(process, node, errors,
                        "Could not parse period '" + timer.getPeriod() + "': " + e.getMessage());
            }
        }

        if (timer.getDate() != null && !timer.getDate().contains("#{")) {
            try {
                DateTimeUtils.parseDateAsDuration(timer.getDate());
            } catch (RuntimeException e) {
                addErrorMessage(process, node, errors,
                        "Could not parse date '" + timer.getDate() + "': " + e.getMessage());
            }
        }
    }

    public ProcessValidationError[] validateProcess(Process process) {
        if (!(process instanceof ExecutableProcess)) {
            throw new IllegalArgumentException("This validator can only validate ruleflow processes!");
        }
        return validateProcess((ExecutableProcess) process);
    }

    private void validateVariables(List<ProcessValidationError> errors, ExecutableProcess process) {

        List<Variable> variables = process.getVariableScope().getVariables();

        if (variables != null) {
            for (Variable var : variables) {
                DataType varDataType = var.getType();
                if (varDataType == null) {
                    errors.add(
                            new ProcessValidationErrorImpl(process, "Variable '" + var.getName() + "' has no type."));
                }
            }
        }
    }

    @Override
    public boolean accept(Process process, Resource resource) {
        return ExecutableProcess.RULEFLOW_TYPE.equals(process.getType());
    }

    protected void validateCompensationIntermediateOrEndEvent(Node node, ExecutableProcess process,
            List<ProcessValidationError> errors) {
        if (node.getMetaData().containsKey("Compensation")) {
            // Validate that activityRef in throw/end compensation event refers to "visible"
            // compensation
            String activityRef = (String) node.getMetaData().get("Compensation");
            Node refNode = null;
            if (activityRef != null) {
                Queue<Node> nodeQueue = new LinkedList<>();
                nodeQueue.addAll(Arrays.asList(process.getNodes()));
                while (!nodeQueue.isEmpty()) {
                    Node polledNode = nodeQueue.poll();
                    if (activityRef.equals(polledNode.getMetaData().get("UniqueId"))) {
                        refNode = polledNode;
                        break;
                    }
                    if (node instanceof NodeContainer) {
                        nodeQueue.addAll(Arrays.asList(((NodeContainer) node).getNodes()));
                    }
                }
            }
            if (refNode == null) {
                addErrorMessage(process, node, errors, "Does not reference an activity that exists (" + activityRef
                        + ") in its compensation event definition.");
            }

            CompensationScope compensationScope = (CompensationScope) ((NodeImpl) node)
                    .resolveContext(CompensationScope.COMPENSATION_SCOPE, activityRef);
            if (compensationScope == null) {
                addErrorMessage(process, node, errors, "References an activity (" + activityRef
                        + ") in its compensation event definition that is not visible to it.");
            }
        }
    }

    @Override
    public boolean compilationSupported() {
        return true;
    }

    public void addErrorMessage(ExecutableProcess process, Node node, List<ProcessValidationError> errors,
            String message) {
        String error = String.format("Node '%s' [%d] %s", node.getName(), node.getId(), message);
        errors.add(new ProcessValidationErrorImpl(process, error));
    }
}