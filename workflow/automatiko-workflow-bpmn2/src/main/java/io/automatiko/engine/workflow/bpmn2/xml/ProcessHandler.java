
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.expression.ExpressionEvaluator;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.FunctionTagDefinition;
import io.automatiko.engine.workflow.base.core.Process;
import io.automatiko.engine.workflow.base.core.StaticTagDefinition;
import io.automatiko.engine.workflow.base.core.TagDefinition;
import io.automatiko.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationHandler;
import io.automatiko.engine.workflow.base.core.context.exception.CompensationScope;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.core.context.swimlane.Swimlane;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.base.instance.impl.actions.CancelNodeInstanceAction;
import io.automatiko.engine.workflow.base.instance.impl.actions.ProcessInstanceCompensationAction;
import io.automatiko.engine.workflow.base.instance.impl.actions.SignalProcessInstanceAction;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.bpmn2.core.DataStore;
import io.automatiko.engine.workflow.bpmn2.core.Definitions;
import io.automatiko.engine.workflow.bpmn2.core.Error;
import io.automatiko.engine.workflow.bpmn2.core.Escalation;
import io.automatiko.engine.workflow.bpmn2.core.Interface;
import io.automatiko.engine.workflow.bpmn2.core.IntermediateLink;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Lane;
import io.automatiko.engine.workflow.bpmn2.core.Message;
import io.automatiko.engine.workflow.bpmn2.core.Resource;
import io.automatiko.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.core.Connection;
import io.automatiko.engine.workflow.process.core.Constraint;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.ConnectionRef;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.ConstraintTrigger;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateBasedNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.Trigger;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;
import io.automatiko.engine.workflow.util.PatternConstants;

public class ProcessHandler extends BaseAbstractHandler implements Handler {

    private static final Logger logger = LoggerFactory.getLogger(ProcessHandler.class);

    public static final String CONNECTIONS = "BPMN.Connections";
    public static final String LINKS = "BPMN.ThrowLinks";
    public static final String ASSOCIATIONS = "BPMN.Associations";
    public static final String ERRORS = "BPMN.Errors";
    public static final String ESCALATIONS = "BPMN.Escalations";

    static final String PROCESS_INSTANCE_SIGNAL_EVENT = "kcontext.getProcessInstance().signalEvent(\"";
    static final String RUNTIME_SIGNAL_EVENT = "kcontext.getKnowledgeRuntime().signalEvent(\"";
    static final String RUNTIME_MANAGER_SIGNAL_EVENT = "((org.kie.api.runtime.manager.RuntimeManager)kcontext.getKnowledgeRuntime().getEnvironment().get(\"RuntimeManager\")).signalEvent(\"";

    @SuppressWarnings("unchecked")
    public ProcessHandler() {
        if ((this.validParents == null) && (this.validPeers == null)) {
            this.validParents = new HashSet();
            this.validParents.add(Definitions.class);

            this.validPeers = new HashSet();
            this.validPeers.add(null);
            this.validPeers.add(ItemDefinition.class);
            this.validPeers.add(Resource.class);
            this.validPeers.add(Message.class);
            this.validPeers.add(Interface.class);
            this.validPeers.add(Escalation.class);
            this.validPeers.add(Error.class);
            this.validPeers.add(Signal.class);
            this.validPeers.add(DataStore.class);
            this.validPeers.add(ExecutableProcess.class);

            this.allowNesting = false;
        }
    }

    public Object start(final String uri, final String localName, final Attributes attrs,
            final ExtensibleXmlParser parser) throws SAXException {
        parser.startElementBuilder(localName, attrs);

        String id = attrs.getValue("id");
        String name = attrs.getValue("name");
        String visibility = attrs.getValue("processType");
        String executable = attrs.getValue("isExecutable");
        String packageName = attrs.getValue("https://automatiko.io", "packageName");
        String dynamic = attrs.getValue("https://automatiko.io", "adHoc");
        String version = attrs.getValue("https://automatiko.io", "version");

        ExecutableProcess process = new ExecutableProcess();
        process.setAutoComplete(true);
        process.setId(id);
        if (name == null) {
            name = id;
        }
        process.setName(name);
        process.setType("RuleFlow");
        if (packageName == null) {
            packageName = "io.automatiko.processes";
        }
        process.setPackageName(packageName);
        if ("true".equals(dynamic)) {
            process.setDynamic(true);
            process.setAutoComplete(false);
        }
        if (executable != null) {
            process.setExecutable(Boolean.parseBoolean(executable));
        }
        if (version != null) {
            process.setVersion(version);
        }
        if (visibility == null || "".equals(visibility)) {
            visibility = WorkflowProcess.NONE_VISIBILITY;
        }
        process.setVisibility(visibility);

        ((ProcessBuildData) parser.getData()).addProcess(process);
        // register the definitions object as metadata of process.
        process.setMetaData("Definitions", parser.getParent());
        // register bpmn2 imports as meta data of process
        Object typedImports = ((ProcessBuildData) parser.getData()).getMetaData("Bpmn2Imports");
        if (typedImports != null) {
            process.setMetaData("Bpmn2Imports", typedImports);
        }
        // register item definitions as meta data of process
        Object itemDefinitions = ((ProcessBuildData) parser.getData()).getMetaData("ItemDefinitions");
        if (itemDefinitions != null) {
            process.setMetaData("ItemDefinitions", itemDefinitions);
        }

        // for unique id's of nodes, start with one to avoid returning wrong nodes for
        // dynamic nodes
        parser.getMetaData().put("idGen", new AtomicInteger(1));

        return process;
    }

    @SuppressWarnings("unchecked")
    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();

        ExecutableProcess process = (ExecutableProcess) parser.getCurrent();
        List<IntermediateLink> throwLinks = (List<IntermediateLink>) process.getMetaData(LINKS);
        linkIntermediateLinks(process, throwLinks);

        List<SequenceFlow> connections = (List<SequenceFlow>) process.getMetaData(CONNECTIONS);
        linkConnections(process, connections);
        linkBoundaryEvents(process);

        // This must be done *after* linkConnections(process, connections)
        // because it adds hidden connections for compensations
        List<Association> associations = (List<Association>) process.getMetaData(ASSOCIATIONS);
        linkAssociations((Definitions) process.getMetaData("Definitions"), process, associations);

        List<Lane> lanes = (List<Lane>) process.getMetaData(LaneHandler.LANES);
        assignLanes(process, lanes);
        postProcessNodes(process, process);

        // process tags if any defined
        processTags(process);

        return process;
    }

    public static void linkIntermediateLinks(NodeContainer process, List<IntermediateLink> links) {

        if (null != links) {

            // Search throw links
            ArrayList<IntermediateLink> throwLinks = new ArrayList<IntermediateLink>();
            for (IntermediateLink aLinks : links) {
                if (aLinks.isThrowLink()) {
                    throwLinks.add(aLinks);
                }
            }

            // Look for catch links for a throw link
            for (IntermediateLink throwLink : throwLinks) {

                ArrayList<IntermediateLink> linksWithSharedNames = new ArrayList<IntermediateLink>();
                for (IntermediateLink aLink : links) {
                    if (throwLink.getName().equals(aLink.getName())) {
                        linksWithSharedNames.add(aLink);
                    }
                }

                if (linksWithSharedNames.size() < 2) {
                    throw new IllegalArgumentException("There should be at least 2 link events to make a connection");
                }

                linksWithSharedNames.remove(throwLink);

                // Make the connections
                Node t = findNodeByIdOrUniqueIdInMetadata(process, throwLink.getUniqueId());

                // connect throw to catch
                for (IntermediateLink catchLink : linksWithSharedNames) {

                    Node c = findNodeByIdOrUniqueIdInMetadata(process, catchLink.getUniqueId());
                    if (t != null && c != null) {
                        Connection result = new ConnectionImpl(t, NodeImpl.CONNECTION_DEFAULT_TYPE, c,
                                NodeImpl.CONNECTION_DEFAULT_TYPE);
                        result.setMetaData("linkNodeHidden", "yes");
                    }
                }

                // Remove processed links
                links.remove(throwLink);
                links.removeAll(linksWithSharedNames);
            }

            if (links.size() > 0) {
                throw new IllegalArgumentException(links.size() + " links were not processed");
            }

        }
    }

    private static Object findNodeOrDataStoreByUniqueId(Definitions definitions, NodeContainer nodeContainer,
            final String nodeRef, String errorMsg) {
        if (definitions != null) {
            List<DataStore> dataStores = definitions.getDataStores();
            if (dataStores != null) {
                for (DataStore dataStore : dataStores) {
                    if (nodeRef.equals(dataStore.getId())) {
                        return dataStore;
                    }
                }
            }
        }
        return findNodeByIdOrUniqueIdInMetadata(nodeContainer, nodeRef, errorMsg);
    }

    private static Node findNodeByIdOrUniqueIdInMetadata(NodeContainer nodeContainer, String targetRef) {
        return findNodeByIdOrUniqueIdInMetadata(nodeContainer, targetRef,
                "Could not find target node for connection:" + targetRef);
    }

    private static Node findNodeByIdOrUniqueIdInMetadata(NodeContainer nodeContainer, final String nodeRef,
            String errorMsg) {
        Node node = null;
        // try looking for a node with same "UniqueId" (in metadata)
        for (Node containerNode : nodeContainer.getNodes()) {
            if (nodeRef.equals(containerNode.getMetaData().get("UniqueId"))) {
                node = containerNode;
                break;
            }
        }
        if (node == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return node;
    }

    public Class<?> generateNodeFor() {
        return ExecutableProcess.class;
    }

    public void linkConnections(NodeContainer nodeContainer, List<SequenceFlow> connections) {
        if (connections != null) {
            for (SequenceFlow connection : connections) {
                String sourceRef = connection.getSourceRef();
                Node source = findNodeByIdOrUniqueIdInMetadata(nodeContainer, sourceRef,
                        "Could not find source node for connection:" + sourceRef);

                if (source instanceof EventNode) {
                    for (EventFilter eventFilter : ((EventNode) source).getEventFilters()) {
                        if (eventFilter instanceof EventTypeFilter) {
                            if ("Compensation".equals(((EventTypeFilter) eventFilter).getType())) {
                                // While this isn't explicitly stated in the spec,
                                // BPMN Method & Style, 2nd Ed. (Silver), states this on P. 131
                                throw new IllegalArgumentException(
                                        "A Compensation Boundary Event can only be *associated* with a compensation activity via an Association, not via a Sequence Flow element.");
                            }
                        }
                    }
                }

                String targetRef = connection.getTargetRef();
                Node target = findNodeByIdOrUniqueIdInMetadata(nodeContainer, targetRef,
                        "Could not find target node for connection:" + targetRef);

                Connection result = new ConnectionImpl(source, NodeImpl.CONNECTION_DEFAULT_TYPE, target,
                        NodeImpl.CONNECTION_DEFAULT_TYPE);
                result.setMetaData("bendpoints", connection.getBendpoints());
                result.setMetaData("UniqueId", connection.getId());

                if ("true".equals(System.getProperty("jbpm.enable.multi.con"))) {
                    NodeImpl nodeImpl = (NodeImpl) source;
                    Constraint constraint = buildConstraint(connection, nodeImpl);
                    if (constraint != null) {
                        nodeImpl.addConstraint(
                                new ConnectionRef(connection.getId(), target.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE),
                                constraint);
                    }

                } else if (source instanceof Split) {
                    Split split = (Split) source;
                    Constraint constraint = buildConstraint(connection, split);
                    split.addConstraint(
                            new ConnectionRef(connection.getId(), target.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE),
                            constraint);
                }
            }
        }
    }

    public void linkBoundaryEvents(NodeContainer nodeContainer) {
        for (Node node : nodeContainer.getNodes()) {
            if (node instanceof EventNode) {
                final String attachedTo = (String) node.getMetaData().get("AttachedTo");
                if (attachedTo != null) {
                    for (EventFilter filter : ((EventNode) node).getEventFilters()) {
                        String type = ((EventTypeFilter) filter).getType();
                        Node attachedNode = findNodeByIdOrUniqueIdInMetadata(nodeContainer, attachedTo,
                                "Could not find node to attach to: " + attachedTo);

                        //
                        if (!(attachedNode instanceof StateBasedNode) && !type.equals("Compensation")) {
                            throw new IllegalArgumentException(
                                    "Boundary events are supported only on StateBasedNode, found node: "
                                            + attachedNode.getClass().getName() + " ["
                                            + attachedNode.getMetaData().get("UniqueId") + "]");
                        }

                        if (type.startsWith("Escalation")) {
                            linkBoundaryEscalationEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.startsWith("Error-")) {
                            linkBoundaryErrorEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.startsWith("Timer-")) {
                            linkBoundaryTimerEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.equals("Compensation")) {
                            linkBoundaryCompensationEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (node.getMetaData().get("SignalName") != null || type.startsWith("Message-")) {
                            linkBoundarySignalEvent(nodeContainer, node, attachedTo, attachedNode);
                        } else if (type.startsWith("Condition-")) {
                            linkBoundaryConditionEvent(nodeContainer, node, attachedTo, attachedNode);
                        }
                    }
                }
            }
        }
    }

    protected void linkBoundaryEscalationEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        boolean cancelActivity = (Boolean) node.getMetaData().get("CancelActivity");
        String escalationCode = (String) node.getMetaData().get("EscalationEvent");
        String escalationStructureRef = (String) node.getMetaData().get("EscalationStructureRef");

        ContextContainer compositeNode = (ContextContainer) attachedNode;
        ExceptionScope exceptionScope = (ExceptionScope) compositeNode
                .getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
        if (exceptionScope == null) {
            exceptionScope = new ExceptionScope();
            compositeNode.addContext(exceptionScope);
            compositeNode.setDefaultContext(exceptionScope);
        }

        String variable = ((EventNode) node).getVariableName();
        ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
        ConsequenceAction action = createJavaAction(
                new SignalProcessInstanceAction("Escalation-" + attachedTo + "-" + escalationCode, variable,
                        SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
        exceptionHandler.setAction(action);
        exceptionHandler.setFaultVariable(variable);
        exceptionScope.setExceptionHandler(escalationCode, exceptionHandler);
        if (escalationStructureRef != null) {
            exceptionScope.setExceptionHandler(escalationStructureRef, exceptionHandler);
        }

        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<ProcessAction>();
            }
            ConsequenceAction cancelAction = new ConsequenceAction("java", "");
            cancelAction.setMetaData("Action", new CancelNodeInstanceAction(attachedTo));
            actions.add(cancelAction);
            ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
        }
    }

    protected void linkBoundaryErrorEvent(NodeContainer nodeContainer, Node node, String attachedTo,
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

        ConsequenceAction action = createJavaAction(new SignalProcessInstanceAction(
                "Error-" + attachedTo + "-" + errorCode, variable, SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
        exceptionHandler.setAction(action);
        exceptionHandler.setFaultVariable(variable);
        if (hasErrorCode) {
            for (String error : errorCode.split(",")) {
                exceptionScope.setExceptionHandler(error, exceptionHandler);
            }
        } else {
            exceptionScope.setExceptionHandler(null, exceptionHandler);
        }
        exceptionHandler.setRetryAfter((Integer) node.getMetaData().get("ErrorRetry"));
        exceptionHandler.setRetryLimit((Integer) node.getMetaData().get("ErrorRetryLimit"));
        exceptionHandler.setRetryIncrement((Integer) node.getMetaData().get("ErrorRetryIncrement"));
        if (node.getMetaData().get("ErrorRetryIncrementMultiplier") != null) {
            exceptionHandler
                    .setRetryIncrementMultiplier(
                            ((Number) node.getMetaData().get("ErrorRetryIncrementMultiplier"))
                                    .floatValue());
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

    protected void linkBoundaryTimerEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        boolean cancelActivity = (Boolean) node.getMetaData().get("CancelActivity");
        StateBasedNode compositeNode = (StateBasedNode) attachedNode;
        String timeDuration = (String) node.getMetaData().get("TimeDuration");
        String timeCycle = (String) node.getMetaData().get("TimeCycle");
        String timeDate = (String) node.getMetaData().get("TimeDate");
        Timer timer = new Timer();
        if (timeDuration != null) {
            timer.setDelay(timeDuration);
            timer.setTimeType(Timer.TIME_DURATION);
            ConsequenceAction consequenceAction = createJavaAction(
                    new SignalProcessInstanceAction("Timer-" + attachedTo + "-" + timeDuration + "-" + node.getId(),
                            kcontext -> kcontext.getNodeInstance().getId(),
                            SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
            compositeNode.addTimer(timer, consequenceAction);
        } else if (timeCycle != null) {
            int index = timeCycle.indexOf("###");
            if (index != -1) {
                String period = timeCycle.substring(index + 3);
                timeCycle = timeCycle.substring(0, index);
                timer.setPeriod(period);
            }
            timer.setDelay(timeCycle);
            timer.setTimeType(Timer.TIME_CYCLE);

            String finalTimeCycle = timeCycle;

            ConsequenceAction action = createJavaAction(new SignalProcessInstanceAction(
                    "Timer-" + attachedTo + "-" + finalTimeCycle
                            + (timer.getPeriod() == null ? "" : "###" + timer.getPeriod()) + "-" + node.getId(),
                    kcontext -> kcontext.getNodeInstance().getId(),
                    SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
            compositeNode.addTimer(timer, action);
        } else if (timeDate != null) {
            timer.setDate(timeDate);
            timer.setTimeType(Timer.TIME_DATE);
            ConsequenceAction action = createJavaAction(
                    new SignalProcessInstanceAction("Timer-" + attachedTo + "-" + timeDate + "-" + node.getId(),
                            kcontext -> kcontext.getNodeInstance().getId(),
                            SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
            compositeNode.addTimer(timer, action);
        }

        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<ProcessAction>();
            }
            ConsequenceAction action = createJavaAction(new CancelNodeInstanceAction(attachedTo));
            actions.add(action);
            ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
        }
    }

    protected void linkBoundaryCompensationEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        /**
         * BPMN2 Spec, p. 264: "For an Intermediate event attached to the boundary of an
         * activity:" ... The Activity the Event is attached to will provide the Id
         * necessary to match the Compensation Event with the Event that threw the
         * compensation"
         * 
         * In other words: "activityRef" is and should be IGNORED
         */

        String activityRef = (String) node.getMetaData().get("ActivityRef");
        if (activityRef != null) {
            logger.warn("Attribute activityRef={} will be IGNORED since this is a Boundary Compensation Event.",
                    activityRef);
        }

        // linkAssociations takes care of the rest
    }

    protected void linkBoundarySignalEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        boolean cancelActivity = (Boolean) node.getMetaData().get("CancelActivity");
        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<ProcessAction>();
            }
            ConsequenceAction action = createJavaAction(new CancelNodeInstanceAction(attachedTo));
            actions.add(action);
            ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
        }
    }

    protected void linkBoundaryConditionEvent(NodeContainer nodeContainer, Node node, String attachedTo,
            Node attachedNode) {
        String processId = ((ExecutableProcess) nodeContainer).getId();
        String eventType = "RuleFlowStateEvent-" + processId + "-" + ((EventNode) node).getUniqueId() + "-"
                + attachedTo;
        ((EventTypeFilter) ((EventNode) node).getEventFilters().get(0)).setType(eventType);
        boolean cancelActivity = (Boolean) node.getMetaData().get("CancelActivity");
        if (cancelActivity) {
            List<ProcessAction> actions = ((EventNode) node).getActions(EndNode.EVENT_NODE_EXIT);
            if (actions == null) {
                actions = new ArrayList<ProcessAction>();
            }
            ConsequenceAction action = createJavaAction(new CancelNodeInstanceAction(attachedTo));
            actions.add(action);
            ((EventNode) node).setActions(EndNode.EVENT_NODE_EXIT, actions);
        }
    }

    public static void linkAssociations(Definitions definitions, NodeContainer nodeContainer,
            List<Association> associations) {
        if (associations != null) {
            for (Association association : associations) {
                String sourceRef = association.getSourceRef();
                Object source = null;
                try {
                    source = findNodeOrDataStoreByUniqueId(definitions, nodeContainer, sourceRef,
                            "Could not find source [" + sourceRef + "] for association " + association.getId() + "]");
                } catch (IllegalArgumentException e) {
                    // source not found
                }
                String targetRef = association.getTargetRef();
                Object target = null;
                try {
                    target = findNodeOrDataStoreByUniqueId(definitions, nodeContainer, targetRef,
                            "Could not find target [" + targetRef + "] for association [" + association.getId() + "]");
                } catch (IllegalArgumentException e) {
                    // target not found
                }
                if (source == null || target == null) {
                    // TODO: ignoring this association for now
                } else if (target instanceof DataStore || source instanceof DataStore) {
                    // TODO: ignoring data store associations for now
                } else if (source instanceof EventNode) {
                    EventNode sourceNode = (EventNode) source;
                    Node targetNode = (Node) target;
                    checkBoundaryEventCompensationHandler(association, sourceNode, targetNode);

                    // make sure IsForCompensation is set to true on target
                    NodeImpl targetNodeImpl = (NodeImpl) target;
                    String isForCompensation = "isForCompensation";
                    Object compensationObject = targetNodeImpl.getMetaData(isForCompensation);
                    if (compensationObject == null) {
                        targetNodeImpl.setMetaData(isForCompensation, true);
                        logger.warn("Setting {} attribute to true for node {}", isForCompensation, targetRef);
                    } else if (!Boolean.parseBoolean(compensationObject.toString())) {
                        throw new IllegalArgumentException(isForCompensation + " attribute [" + compensationObject
                                + "] should be true for Compensation Activity [" + targetRef + "]");
                    }

                    // put Compensation Handler in CompensationHandlerNode
                    NodeContainer sourceParent = sourceNode.getParentContainer();
                    NodeContainer targetParent = targetNode.getParentContainer();
                    if (!sourceParent.equals(targetParent)) {
                        throw new IllegalArgumentException(
                                "Compensation Associations may not cross (sub-)process boundaries,");
                    }

                    // connect boundary event to compensation activity
                    ConnectionImpl connection = new ConnectionImpl(sourceNode, NodeImpl.CONNECTION_DEFAULT_TYPE,
                            targetNode, NodeImpl.CONNECTION_DEFAULT_TYPE);
                    connection.setMetaData("UniqueId", association.getId());
                    connection.setMetaData("hidden", true);
                    connection.setMetaData("association", true);

                    // Compensation use cases:
                    // - boundary event --associated-> activity
                    // - implicit sub process compensation handler + recursive?

                    /**
                     * BPMN2 spec, p.442: "A Compensation Event Sub-process becomes enabled when its
                     * parent Activity transitions into state Completed. At that time, a snapshot of
                     * the data associated with the parent Acitivity is taken and kept for later
                     * usage by the Compensation Event Sub-Process."
                     */
                }
            }
        }
    }

    /**
     * This logic belongs in {@link ExecutableProcessValidator} -- except that
     * {@link Association}s are a jbpm-bpmn2 class, and
     * {@link ExecutableProcessValidator} is a jbpm-flow class..
     * </p>
     * Maybe we should have a BPMNProcessValidator class?
     * 
     * @param association The association to check.
     * @param source The source of the association.
     * @param target The target of the association.
     */
    private static void checkBoundaryEventCompensationHandler(Association association, Node source, Node target) {
        // check that
        // - event node is boundary event node
        if (!(source instanceof BoundaryEventNode)) {
            throw new IllegalArgumentException(
                    "(Compensation) activities may only be associated with Boundary Event Nodes (not with"
                            + source.getClass().getSimpleName() + " nodes [node "
                            + ((String) source.getMetaData().get("UniqueId")) + "].");
        }
        BoundaryEventNode eventNode = (BoundaryEventNode) source;

        // - event node has compensationEvent
        List<EventFilter> eventFilters = eventNode.getEventFilters();
        boolean compensationCheckPassed = false;
        if (eventFilters != null) {
            for (EventFilter filter : eventFilters) {
                if (filter instanceof EventTypeFilter) {
                    String type = ((EventTypeFilter) filter).getType();
                    if (type != null && type.equals("Compensation")) {
                        compensationCheckPassed = true;
                    }
                }
            }
        }

        if (!compensationCheckPassed) {
            throw new IllegalArgumentException(
                    "An Event [" + ((String) eventNode.getMetaData("UniqueId")) + "] linked from an association ["
                            + association.getId() + "] must be a (Boundary) Compensation Event.");
        }

        // - boundary event node is attached to the correct type of node?
        /**
         * Tasks: business: RuleSetNode manual: WorkItemNode receive: WorkItemNode
         * script: ActionNode send: WorkItemNode service: WorkItemNode task:
         * WorkItemNode user: HumanTaskNode
         */
        String attachedToId = eventNode.getAttachedToNodeId();
        Node attachedToNode = null;
        for (Node node : eventNode.getParentContainer().getNodes()) {
            if (attachedToId.equals(node.getMetaData().get("UniqueId"))) {
                attachedToNode = node;
                break;
            }
        }
        if (attachedToNode == null) {
            throw new IllegalArgumentException("Boundary Event [" + ((String) eventNode.getMetaData("UniqueId"))
                    + "] is not attached to a node [" + attachedToId + "] that can be found.");
        }
        if (!(attachedToNode instanceof RuleSetNode || attachedToNode instanceof WorkItemNode
                || attachedToNode instanceof ActionNode || attachedToNode instanceof HumanTaskNode
                || attachedToNode instanceof CompositeNode || attachedToNode instanceof SubProcessNode)) {
            throw new IllegalArgumentException("Compensation Boundary Event ["
                    + ((String) eventNode.getMetaData("UniqueId")) + "] must be attached to a task or sub-process.");
        }

        // - associated node is a task or subProcess
        compensationCheckPassed = false;
        if (target instanceof WorkItemNode || target instanceof HumanTaskNode || target instanceof CompositeContextNode
                || target instanceof SubProcessNode) {
            compensationCheckPassed = true;
        } else if (target instanceof ActionNode) {
            Object nodeTypeObj = ((ActionNode) target).getMetaData("NodeType");
            if (nodeTypeObj != null && nodeTypeObj.equals("ScriptTask")) {
                compensationCheckPassed = true;
            }
        }
        if (!compensationCheckPassed) {
            throw new IllegalArgumentException("An Activity [" + ((String) ((NodeImpl) target).getMetaData("UniqueId"))
                    + "] associated with a Boundary Compensation Event must be a Task or a (non-Event) Sub-Process");
        }

        // - associated node does not have outgoingConnections of it's own
        compensationCheckPassed = true;
        NodeImpl targetNode = (NodeImpl) target;
        Map<String, List<io.automatiko.engine.api.definition.process.Connection>> connectionsMap = targetNode
                .getOutgoingConnections();
        ConnectionImpl outgoingConnection = null;
        for (String connectionType : connectionsMap.keySet()) {
            List<io.automatiko.engine.api.definition.process.Connection> connections = connectionsMap
                    .get(connectionType);
            if (connections != null && !connections.isEmpty()) {
                for (io.automatiko.engine.api.definition.process.Connection connection : connections) {
                    Object hiddenObj = connection.getMetaData().get("hidden");
                    if (hiddenObj != null && ((Boolean) hiddenObj)) {
                        continue;
                    }
                    outgoingConnection = (ConnectionImpl) connection;
                    compensationCheckPassed = false;
                    break;
                }
            }
        }
        if (!compensationCheckPassed) {
            throw new IllegalArgumentException("A Compensation Activity ["
                    + ((String) targetNode.getMetaData("UniqueId")) + "] may not have any outgoing connection ["
                    + (String) outgoingConnection.getMetaData("UniqueId") + "]");
        }
    }

    private void assignLanes(ExecutableProcess process, List<Lane> lanes) {
        List<String> laneNames = new ArrayList<String>();
        Map<String, String> laneMapping = new HashMap<String, String>();
        if (lanes != null) {
            for (Lane lane : lanes) {
                String name = lane.getName();
                if (name != null) {
                    Swimlane swimlane = new Swimlane();
                    swimlane.setName(name);
                    process.getSwimlaneContext().addSwimlane(swimlane);
                    laneNames.add(name);
                    for (String flowElementRef : lane.getFlowElements()) {
                        laneMapping.put(flowElementRef, name);
                    }
                }
            }
        }
        assignLanes(process, laneMapping);
    }

    private void postProcessNodes(ExecutableProcess process, NodeContainer container) {
        List<String> eventSubProcessHandlers = new ArrayList<String>();
        for (Node node : container.getNodes()) {

            if (node instanceof StartNode) {
                List<DataAssociation> associations = ((StartNode) node).getOutAssociations();
                if (associations != null) {

                    for (DataAssociation da : associations) {
                        VariableScope scope = (VariableScope) process.getDefaultContext(VariableScope.VARIABLE_SCOPE);
                        Variable variable = scope.findVariable(da.getTarget());
                        if (variable != null) {
                            da.setTarget(variable.getName());
                        }
                    }
                }
            } else if (node instanceof StateNode) {
                StateNode stateNode = (StateNode) node;
                String condition = (String) stateNode.getMetaData("Condition");
                stateNode.setCondition(context -> {
                    return (boolean) MVEL.executeExpression(condition, context.getProcessInstance().getVariables());
                });
            } else if (node instanceof NodeContainer) {
                // prepare event sub process
                if (node instanceof EventSubProcessNode) {
                    EventSubProcessNode eventSubProcessNode = (EventSubProcessNode) node;

                    Node[] nodes = eventSubProcessNode.getNodes();
                    for (Node subNode : nodes) {
                        // avoids cyclomatic complexity
                        if (subNode == null || !(subNode instanceof StartNode)) {
                            continue;
                        }
                        List<Trigger> triggers = ((StartNode) subNode).getTriggers();
                        if (triggers == null) {
                            continue;
                        }
                        for (Trigger trigger : triggers) {
                            if (trigger instanceof EventTrigger) {
                                final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();

                                for (EventFilter filter : filters) {
                                    if (filter instanceof EventTypeFilter) {
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
                                                faultVariable = findVariable(
                                                        trigger.getInAssociations().get(0).getSources().get(0),
                                                        process.getVariableScope());
                                            }

                                            ActionExceptionHandler exceptionHandler = new ActionExceptionHandler();
                                            ConsequenceAction action = new ConsequenceAction("java", "");
                                            action.setMetaData("Action", new SignalProcessInstanceAction(signalType,
                                                    faultVariable, SignalProcessInstanceAction.PROCESS_INSTANCE_SCOPE));
                                            exceptionHandler.setAction(action);
                                            exceptionHandler.setFaultVariable(faultVariable);
                                            exceptionHandler.setRetryAfter((Integer) subNode.getMetaData().get("ErrorRetry"));
                                            exceptionHandler.setRetryIncrement(
                                                    (Integer) subNode.getMetaData().get("ErrorRetryIncrement"));
                                            if (subNode.getMetaData().get("ErrorRetryIncrementMultiplier") != null) {
                                                exceptionHandler
                                                        .setRetryIncrementMultiplier(
                                                                ((Number) subNode.getMetaData()
                                                                        .get("ErrorRetryIncrementMultiplier"))
                                                                                .floatValue());
                                            }
                                            exceptionHandler
                                                    .setRetryLimit((Integer) subNode.getMetaData().get("ErrorRetryLimit"));
                                            if (faultCode != null) {
                                                String trimmedType = type.replaceFirst(replaceRegExp, "");
                                                for (String error : trimmedType.split(",")) {
                                                    exceptionScope.setExceptionHandler(error, exceptionHandler);
                                                    eventSubProcessHandlers.add(error);
                                                }
                                            } else {
                                                exceptionScope.setExceptionHandler(faultCode, exceptionHandler);
                                            }
                                        } else if (type.equals("Compensation")) {
                                            // 1. Find the parent sub-process to this event sub-process
                                            NodeContainer parentSubProcess;
                                            NodeContainer subProcess = eventSubProcessNode.getParentContainer();
                                            Object isForCompensationObj = eventSubProcessNode
                                                    .getMetaData("isForCompensation");
                                            if (isForCompensationObj == null) {
                                                eventSubProcessNode.setMetaData("isForCompensation", true);
                                                logger.warn(
                                                        "Overriding empty or false value of \"isForCompensation\" attribute on Event Sub-Process ["
                                                                + eventSubProcessNode.getMetaData("UniqueId")
                                                                + "] and setting it to true.");
                                            }
                                            if (subProcess instanceof ExecutableProcess) {
                                                // If jBPM deletes the process (instance) as soon as the process
                                                // completes..
                                                // ..how do you expect to signal compensation on the completed process
                                                // (instance)?!?
                                                throw new IllegalArgumentException(
                                                        "Compensation Event Sub-Processes at the process level are not supported.");
                                            }
                                            parentSubProcess = ((Node) subProcess).getParentContainer();

                                            // 2. The event filter (never fires, purely for dumping purposes) has
                                            // already been added

                                            // 3. Add compensation scope
                                            String compensationHandlerId = (String) ((CompositeNode) subProcess)
                                                    .getMetaData("UniqueId");
                                            addCompensationScope(process, eventSubProcessNode, parentSubProcess,
                                                    compensationHandlerId);
                                        }
                                    }
                                }
                            } else if (trigger instanceof ConstraintTrigger) {
                                ConstraintTrigger constraintTrigger = (ConstraintTrigger) trigger;

                                if (constraintTrigger.getConstraint() != null) {
                                    String processId = ((ExecutableProcess) container).getId();
                                    String type = "RuleFlowStateEventSubProcess-Event-" + processId + "-"
                                            + eventSubProcessNode.getUniqueId();
                                    EventTypeFilter eventTypeFilter = new EventTypeFilter();
                                    eventTypeFilter.setType(type);
                                    eventSubProcessNode.addEvent(eventTypeFilter);
                                    eventSubProcessNode.addEvent("variableChanged");

                                    ((StartNode) subNode).setCondition(context -> {
                                        return (boolean) MVEL.executeExpression(constraintTrigger.getConstraint(),
                                                context.getProcessInstance().getVariables());
                                    });
                                }
                            }
                        }
                    } // for( Node subNode : nodes)
                }
                postProcessNodes(process, (NodeContainer) node);
            } else if (node instanceof EndNode) {
                handleIntermediateOrEndThrowCompensationEvent((EndNode) node);
            } else if (node instanceof ActionNode) {
                handleIntermediateOrEndThrowCompensationEvent((ActionNode) node);
            } else if (node instanceof EventNode) {
                final EventNode eventNode = (EventNode) node;
                if (!(eventNode instanceof BoundaryEventNode)
                        && eventNode.getDefaultIncomingConnections().size() == 0) {
                    throw new IllegalArgumentException(
                            "Event node '" + node.getName() + "' [" + node.getId() + "] has no incoming connection");
                }
            }
        }

        // process fault node to disable termnate parent if there is event subprocess
        // handler
        for (Node node : container.getNodes()) {
            if (node instanceof FaultNode) {
                FaultNode faultNode = (FaultNode) node;
                if (eventSubProcessHandlers.contains(faultNode.getFaultName())) {
                    faultNode.setTerminateParent(false);
                }
            }
        }
    }

    private void assignLanes(NodeContainer nodeContainer, Map<String, String> laneMapping) {
        for (Node node : nodeContainer.getNodes()) {
            String lane = null;
            String uniqueId = (String) node.getMetaData().get("UniqueId");
            if (uniqueId != null) {
                lane = laneMapping.get(uniqueId);
            } else {
                lane = laneMapping.get(XmlBPMNProcessDumper.getUniqueNodeId(node));
            }
            if (lane != null) {
                ((NodeImpl) node).setMetaData("Lane", lane);
                if (node instanceof HumanTaskNode) {
                    ((HumanTaskNode) node).setSwimlane(lane);
                }
            }
            if (node instanceof NodeContainer) {
                assignLanes((NodeContainer) node, laneMapping);
            }
        }
    }

    private static Constraint buildConstraint(SequenceFlow connection, NodeImpl node) {
        if (connection.getExpression() == null) {
            return null;
        }

        Constraint constraint = new ConstraintImpl();
        String defaultConnection = (String) node.getMetaData("Default");
        if (defaultConnection != null && defaultConnection.equals(connection.getId())) {
            constraint.setDefault(true);
        }
        if (connection.getName() != null) {
            constraint.setName(connection.getName());
        } else {
            constraint.setName("");
        }
        if (connection.getType() != null) {
            constraint.setType(connection.getType());
        } else {
            constraint.setType("code");
        }
        if (connection.getLanguage() != null) {
            constraint.setDialect(connection.getLanguage());
        }
        if (connection.getExpression() != null) {
            constraint.setConstraint(connection.getExpression());
        }
        constraint.setPriority(connection.getPriority());

        return constraint;
    }

    protected static void addCompensationScope(final ExecutableProcess process, final Node node,
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

    protected void handleIntermediateOrEndThrowCompensationEvent(ExtendedNodeImpl throwEventNode) {
        if (throwEventNode.getMetaData("compensation-activityRef") != null) {
            String activityRef = (String) throwEventNode.getMetaData().remove("compensation-activityRef");

            NodeContainer nodeParent = (NodeContainer) throwEventNode.getParentContainer();
            if (nodeParent instanceof EventSubProcessNode) {
                boolean compensationEventSubProcess = false;
                List<Trigger> startTriggers = ((EventSubProcessNode) nodeParent).findStartNode().getTriggers();
                CESP_CHECK: for (Trigger trigger : startTriggers) {
                    if (trigger instanceof EventTrigger) {
                        for (EventFilter filter : ((EventTrigger) trigger).getEventFilters()) {
                            if (((EventTypeFilter) filter).getType().equals("Compensation")) {
                                compensationEventSubProcess = true;
                                break CESP_CHECK;
                            }
                        }
                    }
                }
                if (compensationEventSubProcess) {
                    // BPMN2 spec, p. 252, p. 248: intermediate and end compensation event
                    // visibility scope
                    nodeParent = (NodeContainer) ((NodeImpl) nodeParent).getParentContainer();
                }
            }
            String parentId;
            if (nodeParent instanceof ExecutableProcess) {
                parentId = ((ExecutableProcess) nodeParent).getId();
            } else {
                parentId = (String) ((NodeImpl) nodeParent).getMetaData("UniqueId");
            }

            String compensationEvent;
            if (activityRef.length() == 0) {
                // general/implicit compensation
                compensationEvent = CompensationScope.IMPLICIT_COMPENSATION_PREFIX + parentId;
            } else {
                // specific compensation
                compensationEvent = activityRef;
            }
            throwEventNode.setMetaData("CompensationEvent", compensationEvent);
            ConsequenceAction compensationAction = new ConsequenceAction("java", "");
            compensationAction.setMetaData("Action", new ProcessInstanceCompensationAction(compensationEvent));

            if (throwEventNode instanceof ActionNode) {
                ((ActionNode) throwEventNode).setAction(compensationAction);

            } else if (throwEventNode instanceof EndNode) {
                List<ProcessAction> actions = new ArrayList<ProcessAction>();
                actions.add(compensationAction);
                ((EndNode) throwEventNode).setActions(EndNode.EVENT_NODE_ENTER, actions);
            }
            throwEventNode.setMetaData("TriggerType", "Compensation");
        }
    }

    /**
     * Finds the right variable by its name to make sure that when given as id it
     * will be also matched
     * 
     * @param variableName name or id of the variable
     * @param variableScope VariableScope of given process
     * @return returns found variable name or given 'variableName' otherwise
     */
    protected String findVariable(String variableName, VariableScope variableScope) {
        if (variableName == null) {
            return null;
        }

        return variableScope.getVariables().stream().filter(v -> v.matchByIdOrName(variableName)).map(v -> v.getName())
                .findFirst().orElse(variableName);
    }

    public static ConsequenceAction createJavaAction(Action action) {
        ConsequenceAction consequenceAction = new ConsequenceAction("java", "");
        consequenceAction.setMetaData("Action", action);

        return consequenceAction;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void processTags(WorkflowProcess process) {
        String tags = (String) process.getMetaData().get("tags");
        List<TagDefinition> tagDefinitions = new ArrayList<TagDefinition>();
        if (tags != null) {
            String[] tagList = tags.split(",");
            int counter = 0;
            for (String tag : tagList) {
                boolean isExpression = PatternConstants.PARAMETER_MATCHER.matcher(tag).matches();

                if (isExpression) {
                    tagDefinitions
                            .add(new FunctionTagDefinition(String.valueOf(++counter), tag,
                                    (exp, vars) -> {
                                        Map<String, Object> replacements = new HashMap<>();
                                        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(exp);
                                        while (matcher.find()) {
                                            String paramName = matcher.group(1);
                                            ExpressionEvaluator evaluator = (ExpressionEvaluator) ((WorkflowProcessImpl) process)
                                                    .getDefaultContext(ExpressionEvaluator.EXPRESSION_EVALUATOR);

                                            Object value = evaluator.evaluate(paramName, vars.getVariables());
                                            replacements.put(paramName, value);
                                        }
                                        for (Map.Entry<String, Object> replacement : replacements.entrySet()) {
                                            exp = exp.replace("#{" + replacement.getKey() + "}",
                                                    replacement.getValue().toString());
                                        }

                                        return exp;
                                    }));
                } else {
                    tagDefinitions.add(new StaticTagDefinition(String.valueOf(++counter), tag));
                }
            }
        }
        ((Process) process).setTagDefinitions(tagDefinitions);
    }
}
