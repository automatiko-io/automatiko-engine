
package io.automatiko.engine.workflow.process.executable.core;

import static io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl.EVENT_NODE_EXIT;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.ACTION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.ATTACHED_TO;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CANCEL_ACTIVITY;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.SIGNAL_NAME;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TIME_CYCLE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TIME_DATE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.TIME_DURATION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.FunctionTagDefinition;
import io.automatiko.engine.workflow.base.core.StaticTagDefinition;
import io.automatiko.engine.workflow.base.core.TagDefinition;
import io.automatiko.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationHandler;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.swimlane.Swimlane;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.base.instance.impl.actions.CancelNodeInstanceAction;
import io.automatiko.engine.workflow.base.instance.impl.actions.SignalProcessInstanceAction;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.ConstraintTrigger;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateBasedNode;
import io.automatiko.engine.workflow.process.core.node.Trigger;
import io.automatiko.engine.workflow.process.executable.core.factory.EndNodeFactory;
import io.automatiko.engine.workflow.process.executable.core.factory.EventSubProcessNodeFactory;
import io.automatiko.engine.workflow.process.executable.core.factory.StartNodeFactory;
import io.automatiko.engine.workflow.process.executable.core.factory.VariableFactory;
import io.automatiko.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;

public class ExecutableProcessFactory extends ExecutableNodeContainerFactory {

    public static final String METHOD_NAME = "name";
    public static final String METHOD_PACKAGE_NAME = "packageName";
    public static final String METHOD_DYNAMIC = "dynamic";
    public static final String METHOD_VERSION = "version";
    public static final String METHOD_VISIBILITY = "visibility";
    public static final String METHOD_VALIDATE = "validate";
    public static final String METHOD_IMPORTS = "imports";
    public static final String METHOD_GLOBAL = "global";
    public static final String METHOD_VARIABLE = "variable";
    public static final String METHOD_EXEC_TIMEOUT = "executionTimeout";

    private static final Logger logger = LoggerFactory.getLogger(ExecutableProcessFactory.class);

    public static ExecutableProcessFactory createProcess(String id) {
        return new ExecutableProcessFactory(id, ExecutableProcess.WORKFLOW_TYPE, false);
    }

    public static ExecutableProcessFactory createProcess(String id, String type) {
        return new ExecutableProcessFactory(id, type, false);
    }

    public static ExecutableProcessFactory createProcess(String id, String type, boolean isServerless) {
        return new ExecutableProcessFactory(id, type, isServerless);
    }

    protected ExecutableProcessFactory(String id, String type, boolean isServerlessWorkflow) {
        ExecutableProcess process = isServerlessWorkflow ? new ServerlessExecutableProcess() : new ExecutableProcess();
        process.setId(id);
        process.setAutoComplete(true);
        process.setType(type);
        setNodeContainer(process);
    }

    public ExecutableProcess getExecutableProcess() {
        return (ExecutableProcess) getNodeContainer();
    }

    public ExecutableProcessFactory name(String name) {
        getExecutableProcess().setName(name);
        return this;
    }

    public ExecutableProcessFactory visibility(String visibility) {
        getExecutableProcess().setVisibility(visibility);
        return this;
    }

    public ExecutableProcessFactory dynamic(boolean dynamic) {
        getExecutableProcess().setDynamic(dynamic);
        if (dynamic) {
            getExecutableProcess().setAutoComplete(false);
        }
        return this;
    }

    public ExecutableProcessFactory version(String version) {
        getExecutableProcess().setVersion(version);
        return this;
    }

    public ExecutableProcessFactory packageName(String packageName) {
        getExecutableProcess().setPackageName(packageName);
        return this;
    }

    public ExecutableProcessFactory imports(String... imports) {
        getExecutableProcess().addImports(Arrays.asList(imports));
        return this;
    }

    public ExecutableProcessFactory functionImports(String... functionImports) {
        getExecutableProcess().addFunctionImports(Arrays.asList(functionImports));
        return this;
    }

    public ExecutableProcessFactory globals(Map<String, String> globals) {
        getExecutableProcess().setGlobals(globals);
        return this;
    }

    public ExecutableProcessFactory global(String name, String type) {
        Map<String, String> globals = getExecutableProcess().getGlobals();
        if (globals == null) {
            globals = new HashMap<String, String>();
            getExecutableProcess().setGlobals(globals);
        }
        globals.put(name, type);
        return this;
    }

    public VariableFactory variable(String id, String name, DataType type) {
        VariableFactory variableFactory = new VariableFactory(this);
        variableFactory.variable(id, name, type);
        return variableFactory;
    }

    public ExecutableProcessFactory variable(String name, DataType type) {
        return variable(name, type, null);
    }

    public ExecutableProcessFactory variable(String name, DataType type, Object value) {
        return variable(name, type, value, null, null);
    }

    public ExecutableProcessFactory variable(String name, DataType type, String metaDataName, Object metaDataValue) {
        return variable(name, type, null, metaDataName, metaDataValue);
    }

    public ExecutableProcessFactory variable(String name, DataType type, Object value, String metaDataName,
            Object metaDataValue) {
        VariableFactory variableFactory = new VariableFactory(this);
        variableFactory.variable("", name, type, value, metaDataName, metaDataValue);
        return variableFactory.done();
    }

    public ExecutableProcessFactory swimlane(String name) {
        Swimlane swimlane = new Swimlane();
        swimlane.setName(name);
        getExecutableProcess().getSwimlaneContext().addSwimlane(swimlane);
        return this;
    }

    public ExecutableProcessFactory exceptionHandler(String exception, ExceptionHandler exceptionHandler) {
        getExecutableProcess().getExceptionScope().setExceptionHandler(exception, exceptionHandler);
        return this;
    }

    public ExecutableProcessFactory exceptionHandler(String exception, String dialect, String action) {
        ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
        exceptionHandler.setAction(new ConsequenceAction(dialect, action));
        return exceptionHandler(exception, exceptionHandler);
    }

    public ExecutableProcessFactory metaData(String name, Object value) {
        getExecutableProcess().setMetaData(name, value);
        return this;
    }

    public ExecutableProcessFactory tag(String id, String value, BiFunction<String, Map<String, Object>, String> function) {
        Collection<TagDefinition> definitions = getExecutableProcess().getTagDefinitions();

        if (function != null) {
            definitions.add(new FunctionTagDefinition(id, value, function));
        } else {
            definitions.add(new StaticTagDefinition(id, value));
        }

        return this;
    }

    public ExecutableProcessFactory validate() {
        link();
        ProcessValidationError[] errors = ExecutableProcessValidator.getInstance()
                .validateProcess(getExecutableProcess());
        for (ProcessValidationError error : errors) {
            logger.error(error.toString());
        }
        if (errors.length > 0) {
            throw new RuntimeException("Process could not be validated !");
        }
        return this;
    }

    public ExecutableProcessFactory link() {
        ExecutableProcess process = getExecutableProcess();
        linkBoundaryEvents(process);
        postProcessNodes(process, process);
        return this;
    }

    public ExecutableProcessFactory done() {
        throw new IllegalArgumentException("Already on the top-level.");
    }

    public ExecutableProcess getProcess() {
        return getExecutableProcess();
    }

    @Override
    public ExecutableProcessFactory connection(long fromId, long toId) {
        super.connection(fromId, toId);
        return this;
    }

    @Override
    public ExecutableProcessFactory connection(long fromId, long toId, String uniqueId) {
        super.connection(fromId, toId, uniqueId);
        return this;
    }

    @Override
    public ExecutableProcessFactory connection(long fromId, long toId, String uniqueId, boolean association) {
        super.connection(fromId, toId, uniqueId, association);
        return this;
    }

    public ExecutableProcessFactory executionTimeout(int nodeIdCounter, String timeoutExpression,
            long... extranodes) {
        int nodeId = ++nodeIdCounter;
        EventSubProcessNodeFactory eventSubProcessNode4 = eventSubProcessNode(nodeId);
        eventSubProcessNode4.name("Execution timeout");
        eventSubProcessNode4.metaData("UniqueId", "SubProcess_" + nodeId);
        eventSubProcessNode4.metaData("hidden_node", true);
        eventSubProcessNode4.keepActive(true);
        eventSubProcessNode4.event("Timer-" + nodeId);
        eventSubProcessNode4.autoComplete(true);

        int startNodeId = ++nodeIdCounter;
        StartNodeFactory startNode5 = eventSubProcessNode4.startNode(startNodeId);
        startNode5.name("Execution timeout :: start");
        startNode5.interrupting(true);
        startNode5.metaData("UniqueId", "StartEvent_" + startNodeId);
        startNode5.metaData("TriggerType", "Timer");
        startNode5.done();
        startNode5.timer(timeoutExpression, null, null, 1);

        int endNodeId = ++nodeIdCounter;
        EndNodeFactory endNode7 = eventSubProcessNode4.endNode(endNodeId);
        endNode7.name("Execution timeout :: end");
        endNode7.terminate(false);
        endNode7.metaData("UniqueId", "EndEvent_" + endNodeId);
        endNode7.done();

        if (extranodes != null && extranodes.length > 0) {
            for (long extraNodId : extranodes) {
                Node node = getNodeContainer().getNode(extraNodId);
                getNodeContainer().removeNode(node);

                eventSubProcessNode4.getNodeContainer().addNode(node);
            }
            if (extranodes.length == 1) {
                eventSubProcessNode4.connection(startNodeId, extranodes[0], "SequenceFlow_e_" + startNodeId);
                eventSubProcessNode4.connection(extranodes[0], endNodeId, "SequenceFlow_e_" + endNodeId);
            } else {

                eventSubProcessNode4.connection(startNodeId, extranodes[0], "SequenceFlow_e_" + startNodeId);
                int counter = 1;
                for (long extraNodId : extranodes) {
                    if (counter > extranodes.length) {
                        eventSubProcessNode4.connection(extraNodId, extranodes[counter], "SequenceFlow_e_" + counter);
                        counter++;
                    }
                }

                eventSubProcessNode4.connection(extranodes[extranodes.length - 1], endNodeId, "SequenceFlow_e_" + endNodeId);
            }
        } else {
            eventSubProcessNode4.connection(startNodeId, endNodeId, "SequenceFlow_" + startNodeId);
        }
        eventSubProcessNode4.done();

        return this;
    }

    protected void linkBoundaryEvents(NodeContainer nodeContainer) {
        for (Node node : nodeContainer.getNodes()) {
            if (node instanceof CompositeNode) {
                CompositeNode compositeNode = (CompositeNode) node;
                linkBoundaryEvents(compositeNode.getNodeContainer());
            }
            if (node instanceof EventNode) {
                final String attachedTo = (String) node.getMetaData().get(ATTACHED_TO);
                if (attachedTo != null) {
                    Node attachedNode = findNodeByIdOrUniqueIdInMetadata(nodeContainer, attachedTo,
                            "Could not find node to attach to: " + attachedTo);
                    for (EventFilter filter : ((EventNode) node).getEventFilters()) {
                        String type = ((EventTypeFilter) filter).getType();
                        if (type.startsWith("Timer-")) {
                            linkBoundaryTimerEvent(node, attachedTo, attachedNode);
                        } else if (node.getMetaData().get(SIGNAL_NAME) != null || type.startsWith("Message-")) {
                            linkBoundarySignalEvent(node, attachedTo);
                        } else if (type.startsWith("Error-")) {
                            linkBoundaryErrorEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.startsWith("Condition-") || type.startsWith("RuleFlowStateEvent-")) {
                            linkBoundaryConditionEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.startsWith("Compensation")) {
                            addCompensationScope(getExecutableProcess(), node, nodeContainer, attachedTo);
                        }
                    }
                }
            }
        }
    }

    protected void linkBoundaryTimerEvent(Node node, String attachedTo, Node attachedNode) {
        boolean cancelActivity = (Boolean) node.getMetaData().get(CANCEL_ACTIVITY);
        StateBasedNode compositeNode = (StateBasedNode) attachedNode;
        String timeDuration = (String) node.getMetaData().get(TIME_DURATION);
        String timeCycle = (String) node.getMetaData().get(TIME_CYCLE);
        String timeDate = (String) node.getMetaData().get(TIME_DATE);
        Timer timer = new Timer();
        if (timeDuration != null) {
            timer.setDelay(timeDuration);
            timer.setTimeType(Timer.TIME_DURATION);
            compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeDuration + "-" + node.getId()));
        } else if (timeCycle != null) {
            int index = timeCycle.indexOf("###");
            if (index != -1) {
                String period = timeCycle.substring(index + 3);
                timeCycle = timeCycle.substring(0, index);
                timer.setPeriod(period);
            }
            timer.setDelay(timeCycle);
            timer.setTimeType(Timer.TIME_CYCLE);
            compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeCycle
                    + (timer.getPeriod() == null ? "" : "###" + timer.getPeriod()) + "-" + node.getId()));
        } else if (timeDate != null) {
            timer.setDate(timeDate);
            timer.setTimeType(Timer.TIME_DATE);
            compositeNode.addTimer(timer, timerAction("Timer-" + attachedTo + "-" + timeDate + "-" + node.getId()));
        }

        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<>();
            }
            ConsequenceAction cancelAction = new ConsequenceAction("java", null);
            cancelAction.setMetaData(ACTION, new CancelNodeInstanceAction(attachedTo));
            actions.add(cancelAction);
            ((EventNode) node).setActions(EVENT_NODE_EXIT, actions);
        }
    }

    protected void linkBoundarySignalEvent(Node node, String attachedTo) {
        boolean cancelActivity = (Boolean) node.getMetaData().get(CANCEL_ACTIVITY);
        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<>();
            }
            ConsequenceAction action = new ConsequenceAction("java", null);
            action.setMetaData(ACTION, new CancelNodeInstanceAction(attachedTo));
            actions.add(action);
            ((EventNode) node).setActions(EVENT_NODE_EXIT, actions);
        }
    }

    private static void linkBoundaryErrorEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        ContextContainer compositeNode = (ContextContainer) attachedNode;
        ExceptionScope exceptionScope = (ExceptionScope) compositeNode
                .getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
        if (exceptionScope == null) {
            exceptionScope = new ExceptionScope();
            compositeNode.addContext(exceptionScope);
            compositeNode.setDefaultContext(exceptionScope);
        }
        String errorCode = (String) node.getMetaData().get("ErrorEvent");
        boolean hasErrorCode = (Boolean) node.getMetaData().get("HasErrorEvent");
        String errorStructureRef = (String) node.getMetaData().get("ErrorStructureRef");
        ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();

        String variable = ((EventNode) node).getVariableName();
        ConsequenceAction action = new ConsequenceAction("java", null);
        action.setMetaData(ACTION, new SignalProcessInstanceAction("Error-" + attachedTo + "-" + errorCode, variable,
                SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
        exceptionHandler.setAction(action);
        exceptionHandler.setFaultVariable(variable);
        exceptionHandler.setRetryAfter((Integer) node.getMetaData().get("ErrorRetry"));
        exceptionHandler.setRetryIncrement((Integer) node.getMetaData().get("ErrorRetryIncrement"));
        if (node.getMetaData().get("ErrorRetryIncrementMultiplier") != null) {
            exceptionHandler
                    .setRetryIncrementMultiplier(
                            ((Number) node.getMetaData().get("ErrorRetryIncrementMultiplier")).floatValue());
        }
        exceptionHandler.setRetryLimit((Integer) node.getMetaData().get("ErrorRetryLimit"));
        if (hasErrorCode) {
            for (String error : errorCode.split(",")) {
                exceptionScope.setExceptionHandler(error, exceptionHandler);
            }
        } else {
            exceptionScope.setExceptionHandler(null, exceptionHandler);
        }
        if (errorStructureRef != null) {
            exceptionScope.setExceptionHandler(errorStructureRef, exceptionHandler);
        }

        List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
        if (actions == null) {
            actions = new ArrayList<ProcessAction>();
        }
        ConsequenceAction cancelAction = new ConsequenceAction("java", null);
        cancelAction.setMetaData("Action", new CancelNodeInstanceAction(attachedTo));
        actions.add(cancelAction);
        ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
    }

    private void linkBoundaryConditionEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        String processId = ((ExecutableProcess) nodeContainer).getId();
        String eventType = "RuleFlowStateEvent-" + processId + "-" + ((EventNode) node).getUniqueId() + "-"
                + attachedTo;
        ((EventTypeFilter) ((EventNode) node).getEventFilters().get(0)).setType(eventType);

        ((ExtendedNodeImpl) attachedNode).setCondition(((EventNode) node).getCondition());
        ((ExtendedNodeImpl) attachedNode).setMetaData("ConditionEventType", eventType);

        boolean cancelActivity = (Boolean) node.getMetaData().get("CancelActivity");
        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<ProcessAction>();
            }
            ConsequenceAction consequenceAction = new ConsequenceAction("java", "");
            consequenceAction.setMetaData("Action", new CancelNodeInstanceAction(attachedTo));
            actions.add(consequenceAction);
            ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
        }
    }

    protected ProcessAction timerAction(String type) {
        ProcessAction signal = new ProcessAction();

        Action action = kcontext -> kcontext.getProcessInstance().signalEvent(type, kcontext.getNodeInstance().getId());
        signal.wire(action);

        return signal;
    }

    protected Node findNodeByIdOrUniqueIdInMetadata(NodeContainer nodeContainer, final String nodeRef,
            String errorMsg) {
        Node node = null;
        // try looking for a node with same "UniqueId" (in metadata)
        for (Node containerNode : nodeContainer.getNodes()) {
            if (nodeRef.equals(containerNode.getMetaData().get(UNIQUE_ID))) {
                node = containerNode;
                break;
            }
        }
        if (node == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return node;
    }

    private void postProcessNodes(ExecutableProcess process, NodeContainer container) {
        List<String> eventSubProcessHandlers = new ArrayList<String>();
        for (Node node : container.getNodes()) {
            if (node instanceof NodeContainer) {
                // prepare event sub process
                if (node instanceof EventSubProcessNode) {
                    EventSubProcessNode eventSubProcessNode = (EventSubProcessNode) node;

                    Node[] nodes = eventSubProcessNode.getNodes();
                    for (Node subNode : nodes) {
                        // avoids cyclomatic complexity
                        if (subNode instanceof StartNode) {

                            processEventSubprocessStartNode(process, ((StartNode) subNode), eventSubProcessNode,
                                    eventSubProcessHandlers);
                        }
                    }
                }
                postProcessNodes(process, (NodeContainer) node);
            }
        }
        // process fault node to disable termnate parent if there is event subprocess handler
        for (Node node : container.getNodes()) {
            if (node instanceof FaultNode) {
                FaultNode faultNode = (FaultNode) node;
                if (eventSubProcessHandlers.contains(faultNode.getFaultName())) {
                    faultNode.setTerminateParent(false);
                }
            }
        }
    }

    private void processEventSubprocessStartNode(ExecutableProcess process, StartNode subNode,
            EventSubProcessNode eventSubProcessNode, List<String> eventSubProcessHandlers) {
        List<Trigger> triggers = subNode.getTriggers();
        if (triggers != null) {

            for (Trigger trigger : triggers) {
                if (trigger instanceof EventTrigger) {
                    final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();

                    for (EventFilter filter : filters) {
                        eventSubProcessNode.addEvent((EventTypeFilter) filter);

                        String type = ((EventTypeFilter) filter).getType();
                        if (type.startsWith("Error-") || type.startsWith("Escalation")) {
                            String faultCode = (String) subNode.getMetaData().get("FaultCode");
                            String replaceRegExp = "Error-|Escalation-";
                            final String signalType = type;

                            ExceptionScope exceptionScope = (ExceptionScope) ((ContextContainer) eventSubProcessNode
                                    .getParentContainer())
                                            .getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
                            if (exceptionScope == null) {
                                exceptionScope = new ExceptionScope();
                                ((ContextContainer) eventSubProcessNode.getParentContainer())
                                        .addContext(exceptionScope);
                                ((ContextContainer) eventSubProcessNode.getParentContainer())
                                        .setDefaultContext(exceptionScope);
                            }
                            String faultVariable = null;
                            if (trigger.getInAssociations() != null
                                    && !trigger.getInAssociations().isEmpty()) {
                                faultVariable = findVariable(trigger.getInAssociations().get(0).getSources().get(0),
                                        process.getVariableScope());
                            }

                            ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
                            ConsequenceAction action = new ConsequenceAction("java", "");
                            action.setMetaData("Action", new SignalProcessInstanceAction(signalType,
                                    faultVariable, SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
                            exceptionHandler.setAction(action);
                            exceptionHandler.setFaultVariable(faultVariable);
                            exceptionHandler.setRetryAfter((Integer) subNode.getMetaData().get("ErrorRetry"));
                            exceptionHandler.setRetryIncrement((Integer) subNode.getMetaData().get("ErrorRetryIncrement"));
                            if (subNode.getMetaData().get("ErrorRetryIncrementMultiplier") != null) {
                                exceptionHandler
                                        .setRetryIncrementMultiplier(
                                                ((Number) subNode.getMetaData().get("ErrorRetryIncrementMultiplier"))
                                                        .floatValue());
                            }
                            exceptionHandler.setRetryLimit((Integer) subNode.getMetaData().get("ErrorRetryLimit"));
                            if (faultCode != null) {
                                String trimmedType = type.replaceFirst(replaceRegExp, "");
                                for (String error : trimmedType.split(",")) {
                                    exceptionScope.setExceptionHandler(error, exceptionHandler);
                                    eventSubProcessHandlers.add(error);
                                }
                            } else {
                                exceptionScope.setExceptionHandler(faultCode, exceptionHandler);
                            }
                        } else if (trigger instanceof ConstraintTrigger) {
                            ConstraintTrigger constraintTrigger = (ConstraintTrigger) trigger;

                            if (constraintTrigger.getConstraint() != null) {
                                EventTypeFilter eventTypeFilter = new EventTypeFilter();
                                eventTypeFilter.setType(type);
                                eventSubProcessNode.addEvent(eventTypeFilter);
                            }
                        }
                    }
                }
            }
        }
    }

    protected String findVariable(String variableName, VariableScope variableScope) {
        if (variableName == null) {
            return null;
        }

        return variableScope.getVariables().stream().filter(v -> v.matchByIdOrName(variableName)).map(v -> v.getName())
                .findFirst().orElse(variableName);
    }

    protected void addCompensationScope(final ExecutableProcess process, final Node node,
            final io.automatiko.engine.api.definition.process.NodeContainer parentContainer,
            final String compensationHandlerId) {
        process.getMetaData().put("Compensation", true);

        assert parentContainer instanceof ContextContainer : "Expected parent node to be a CompositeContextNode, not a "
                + parentContainer.getClass().getSimpleName();

        ContextContainer contextContainer = (ContextContainer) parentContainer;
        CompensationScope scope = null;
        boolean addScope = false;
        if (contextContainer.getContexts(CompensationScope.COMPENSATION_SCOPE) == null) {
            addScope = true;
        } else {
            scope = (CompensationScope) contextContainer.getContexts(CompensationScope.COMPENSATION_SCOPE).get(0);
            if (scope == null) {
                addScope = true;
            }
        }
        if (addScope) {
            scope = new CompensationScope();
            contextContainer.addContext(scope);
            contextContainer.setDefaultContext(scope);
            scope.setContextContainer(contextContainer);
        }

        CompensationHandler handler = new CompensationHandler();
        handler.setNode(node);
        if (scope.getExceptionHandler(compensationHandlerId) != null) {
            throw new IllegalArgumentException("More than one compensation handler per node (" + compensationHandlerId
                    + ")" + " is not supported!");
        }
        scope.setExceptionHandler(compensationHandlerId, handler);
    }

}
