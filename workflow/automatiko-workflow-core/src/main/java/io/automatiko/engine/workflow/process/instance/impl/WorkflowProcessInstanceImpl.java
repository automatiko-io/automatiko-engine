
package io.automatiko.engine.workflow.process.instance.impl;

import static io.automatiko.engine.api.workflow.flexible.ItemDescription.Status.ACTIVE;
import static io.automatiko.engine.api.workflow.flexible.ItemDescription.Status.AVAILABLE;
import static io.automatiko.engine.api.workflow.flexible.ItemDescription.Status.COMPLETED;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.COMPENSATION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CONDITION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CORRELATION_KEY;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CUSTOM_ASYNC;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CUSTOM_SLA_DUE_DATE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.EVENT_TYPE_SIGNAL;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.IS_FOR_COMPENSATION;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;
import static io.automatiko.engine.workflow.process.instance.impl.DummyEventListener.EMPTY_EVENT_LISTENER;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.TrustedIdentityProvider;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.jobs.DurationExpirationTime;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.NodeInstanceContainer;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.workflow.BaseEventDescription;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.api.workflow.ExecutionsErrorInfo;
import io.automatiko.engine.api.workflow.NamedDataType;
import io.automatiko.engine.api.workflow.Tag;
import io.automatiko.engine.api.workflow.flexible.AdHocFragment;
import io.automatiko.engine.api.workflow.flexible.ItemDescription;
import io.automatiko.engine.api.workflow.flexible.Milestone;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.services.correlation.CorrelationKey;
import io.automatiko.engine.services.time.TimerInstance;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.Process;
import io.automatiko.engine.workflow.base.core.TagDefinition;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.ContextInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.TagInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.ProcessInstanceImpl;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.DynamicNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventNodeInterface;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.MilestoneNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateBasedNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstance;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.node.CompositeNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EndNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventBasedNodeInstanceInterface;
import io.automatiko.engine.workflow.process.instance.node.EventNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.EventNodeInstanceInterface;
import io.automatiko.engine.workflow.process.instance.node.EventSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.FaultNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.StartNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.StateBasedNodeInstance;
import io.automatiko.engine.workflow.util.PatternConstants;

/**
 * Default implementation of a RuleFlow process instance.
 */
public abstract class WorkflowProcessInstanceImpl extends ProcessInstanceImpl
        implements WorkflowProcessInstance, io.automatiko.engine.workflow.process.instance.NodeInstanceContainer {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(WorkflowProcessInstanceImpl.class);

    private final List<NodeInstance> nodeInstances = new ArrayList<>();

    private Map<String, List<EventListener>> eventListeners = new HashMap<>();
    private Map<String, List<EventListener>> externalEventListeners = new HashMap<>();

    private List<String> completedNodeIds = new ArrayList<>();
    private List<String> activatingNodeIds;
    private Map<String, Integer> iterationLevels = new HashMap<>();
    private int currentLevel;

    private Object faultData;

    private boolean signalCompletion = true;

    private String deploymentId;
    private String correlationKey;

    private Date startDate;
    private Date endDate;

    private String nodeIdInError;
    private List<ExecutionsErrorInfo> errors = new ArrayList<>();

    private int slaCompliance = SLA_NA;
    private Date slaDueDate;
    private String slaTimerId;

    private String referenceId;

    private String referenceFromRoot;

    private String initiator;

    private Collection<Tag> tags = new LinkedHashSet<Tag>();

    @Override
    public NodeContainer getNodeContainer() {
        return getWorkflowProcess();
    }

    @Override
    public void addNodeInstance(final NodeInstance nodeInstance) {
        if (nodeInstance.getId() == null) {
            // assign new id only if it does not exist as it might already be set by
            // marshalling
            // it's important to keep same ids of node instances as they might be references
            // e.g. exclusive group
            ((NodeInstanceImpl) nodeInstance).setId(UUID.randomUUID().toString());
        }
        this.nodeInstances.add(nodeInstance);
    }

    @Override
    public int getLevelForNode(String uniqueID) {
        if (Boolean.parseBoolean(System.getProperty("jbpm.loop.level.disabled"))) {
            return 1;
        }

        Integer value = iterationLevels.get(uniqueID);
        if (value == null && currentLevel == 0) {
            value = 1;
        } else if ((value == null && currentLevel > 0) || (value != null && currentLevel > 0 && value > currentLevel)) {
            value = currentLevel;
        } else {
            value++;
        }

        iterationLevels.put(uniqueID, value);
        return value;
    }

    @Override
    public void removeNodeInstance(final NodeInstance nodeInstance) {
        this.nodeInstances.remove(nodeInstance);
    }

    @Override
    public Collection<io.automatiko.engine.api.runtime.process.NodeInstance> getNodeInstances() {
        return new ArrayList<>(getNodeInstances(false));
    }

    @Override
    public Collection<NodeInstance> getNodeInstances(boolean recursive) {
        Collection<NodeInstance> result = nodeInstances;
        if (recursive) {
            result = new ArrayList<>(result);
            for (NodeInstance nodeInstance : nodeInstances) {
                if (nodeInstance instanceof NodeInstanceContainer) {
                    result.addAll(((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) nodeInstance)
                            .getNodeInstances(true));
                }
            }
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public NodeInstance getNodeInstance(String nodeInstanceId) {
        return getNodeInstance(nodeInstanceId, false);
    }

    @Override
    public NodeInstance getNodeInstance(String nodeInstanceId, boolean recursive) {
        return getNodeInstances(recursive).stream()
                .filter(nodeInstance -> Objects.equals(nodeInstance.getId(), nodeInstanceId)).findFirst().orElse(null);
    }

    public List<String> getActiveNodeIds() {
        List<String> result = new ArrayList<>();
        addActiveNodeIds(this, result);
        return result;
    }

    private void addActiveNodeIds(NodeInstanceContainer container, List<String> result) {
        for (io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance : container.getNodeInstances()) {
            result.add(((NodeImpl) nodeInstance.getNode()).getUniqueId());
            if (nodeInstance instanceof NodeInstanceContainer) {
                addActiveNodeIds((NodeInstanceContainer) nodeInstance, result);
            }
        }
    }

    @Override
    public NodeInstance getFirstNodeInstance(final long nodeId) {
        for (final NodeInstance nodeInstance : this.nodeInstances) {
            if (nodeInstance.getNodeId() == nodeId && nodeInstance.getLevel() == getCurrentLevel()) {
                return nodeInstance;
            }
        }
        return null;
    }

    public List<NodeInstance> getNodeInstances(final long nodeId) {
        List<NodeInstance> result = new ArrayList<>();
        for (final NodeInstance nodeInstance : getNodeInstances(true)) {
            if (nodeInstance.getNodeId() == nodeId) {
                result.add(nodeInstance);
            }
        }
        return result;
    }

    public List<NodeInstance> getNodeInstances(final long nodeId, final List<NodeInstance> currentView) {
        List<NodeInstance> result = new ArrayList<>();
        for (final NodeInstance nodeInstance : currentView) {
            if (nodeInstance.getNodeId() == nodeId) {
                result.add(nodeInstance);
            }
        }
        return result;
    }

    public NodeInstance getNodeInstanceByNodeDefinitionId(final String nodeDefinitionId, NodeContainer nodeContainer) {

        for (Node node : nodeContainer.getNodes()) {

            if (nodeDefinitionId.equals(node.getMetaData().get(UNIQUE_ID))) {
                return getNodeInstance(node);
            }

            if (node instanceof NodeContainer) {
                NodeInstance ni = getNodeInstanceByNodeDefinitionId(nodeDefinitionId, ((NodeContainer) node));

                if (ni != null) {
                    return ni;
                }
            }
        }

        return null;
    }

    @Override
    public NodeInstance getNodeInstance(final Node node) {
        NodeInstanceFactory conf = NodeInstanceFactoryRegistry.getInstance().getProcessNodeInstanceFactory(node);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal node type: " + node.getClass());
        }
        NodeInstanceImpl nodeInstance = (NodeInstanceImpl) conf.getNodeInstance(node, this, this);

        return nodeInstance;
    }

    public WorkflowProcess getWorkflowProcess() {
        return (WorkflowProcess) getProcess();
    }

    @Override
    public Object getVariable(String name) {
        // for disconnected process instances, try going through the variable scope
        // instances
        // (as the default variable scope cannot be retrieved as the link to the process
        // could
        // be null and the associated working memory is no longer accessible)
        if (getProcessRuntime() == null) {
            List<ContextInstance> variableScopeInstances = getContextInstances(VariableScope.VARIABLE_SCOPE);
            if (variableScopeInstances != null && variableScopeInstances.size() == 1) {
                for (ContextInstance contextInstance : variableScopeInstances) {
                    Object value = ((VariableScopeInstance) contextInstance).getVariable(name);
                    if (value != null) {
                        return value;
                    }
                }
            }
            return null;
        }
        // else retrieve the variable scope
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) getContextInstance(
                VariableScope.VARIABLE_SCOPE);
        if (variableScopeInstance == null) {
            return null;
        }
        return variableScopeInstance.getVariable(name);
    }

    @Override
    public Map<String, Object> getVariables() {
        // for disconnected process instances, try going through the variable scope
        // instances
        // (as the default variable scope cannot be retrieved as the link to the process
        // could
        // be null and the associated working memory is no longer accessible)
        if (getProcessRuntime() == null) {
            List<ContextInstance> variableScopeInstances = getContextInstances(VariableScope.VARIABLE_SCOPE);
            if (variableScopeInstances == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new HashMap<>();
            for (ContextInstance contextInstance : variableScopeInstances) {
                Map<String, Object> variables = ((VariableScopeInstance) contextInstance).getVariables();
                result.putAll(variables);
            }
            return result;
        }
        // else retrieve the variable scope
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) getContextInstance(
                VariableScope.VARIABLE_SCOPE);
        if (variableScopeInstance == null) {
            return null;
        }
        return variableScopeInstance.getVariables();
    }

    @Override
    public Map<String, Object> getPublicVariables() {
        Map<String, Object> variables = new HashMap<>(getVariables());

        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.process.core.WorkflowProcess) getProcess())
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

        for (Variable variable : variableScope.getVariables()) {
            if (variable.hasTag(Variable.SENSITIVE_TAG)) {
                variables.remove(variable.getName());
            }
        }

        return variables;
    }

    @Override
    public void setVariable(String name, Object value) {
        VariableScope variableScope = (VariableScope) ((ContextContainer) getProcess())
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) getContextInstance(
                VariableScope.VARIABLE_SCOPE);
        if (variableScopeInstance == null) {
            throw new IllegalArgumentException("No variable scope found.");
        }
        variableScope.validateVariable(getProcessName(), name, value);
        variableScopeInstance.setVariable(name, value);
    }

    @Override
    public void setState(final int state, String outcome, Object faultData) {
        this.faultData = faultData;
        setState(state, outcome);
    }

    @Override
    public void setState(final int state, String outcome) {
        // TODO move most of this to ProcessInstanceImpl
        if (state == ProcessInstance.STATE_COMPLETED || state == ProcessInstance.STATE_ABORTED) {
            this.endDate = new Date();
            if (this.slaCompliance == SLA_PENDING) {
                if (System.currentTimeMillis() > slaDueDate.getTime()) {
                    // completion of the process instance is after expected SLA due date, mark it
                    // accordingly
                    this.slaCompliance = SLA_VIOLATED;
                } else {
                    this.slaCompliance = state == ProcessInstance.STATE_COMPLETED ? SLA_MET : SLA_ABORTED;
                }
            }

            InternalProcessRuntime processRuntime = getProcessRuntime();
            processRuntime.getProcessEventSupport().fireBeforeProcessCompleted(this, processRuntime);
            // JBPM-8094 - set state after event
            super.setState(state, outcome);

            // deactivate all node instances of this process instance
            while (!nodeInstances.isEmpty()) {
                NodeInstance nodeInstance = nodeInstances.get(0);
                nodeInstance.cancel();
            }
            if (this.slaTimerId != null && !slaTimerId.trim().isEmpty()) {
                processRuntime.getJobsService().cancelJob(this.slaTimerId);
                logger.debug("SLA Timer {} has been canceled", this.slaTimerId);
            }
            removeEventListeners();
            processRuntime.getProcessInstanceManager().removeProcessInstance(this);
            processRuntime.getProcessEventSupport().fireAfterProcessCompleted(this, processRuntime);

            if (isSignalCompletion()) {
                IdentityProvider identity = IdentityProvider.get();
                try {
                    // make sure that identity is switched to trusted one as whoever executed this instance 
                    // might not have access to parent process instance 
                    IdentityProvider.set(new TrustedIdentityProvider("system"));
                    List<EventListener> listeners = eventListeners.get("processInstanceCompleted:" + getId());
                    if (listeners != null) {
                        for (EventListener listener : listeners) {
                            listener.signalEvent("processInstanceCompleted:" + getId(), this);
                        }
                    }

                    processRuntime.getSignalManager().signalEvent("processInstanceCompleted:" + getId(), this);
                } finally {
                    IdentityProvider.set(identity);
                }
            }
        } else {
            super.setState(state, outcome);
        }
    }

    @Override
    public void setState(final int state) {
        setState(state, null);
    }

    @Override
    public void disconnect() {
        getMetaData().remove("ATK_FUNC_FLOW_COUNTER");
        getMetaData().remove("ATK_FUNC_FLOW_NEXT");
        removeEventListeners();
        unregisterExternalEventNodeListeners();

        for (NodeInstance nodeInstance : nodeInstances) {
            if (nodeInstance instanceof EventBasedNodeInstanceInterface) {
                ((EventBasedNodeInstanceInterface) nodeInstance).removeEventListeners();
            }
        }
        super.disconnect();
    }

    @Override
    public void reconnect() {

        super.reconnect();
        for (NodeInstance nodeInstance : nodeInstances) {
            if (nodeInstance instanceof EventBasedNodeInstanceInterface) {
                ((EventBasedNodeInstanceInterface) nodeInstance).addEventListeners();
            }
            if (nodeInstance instanceof CompositeNodeInstance) {
                ((CompositeNodeInstance) nodeInstance).registerExternalEventNodeListeners();
            }

        }
        registerExternalEventNodeListeners();
    }

    @Override
    public String toString() {
        return new StringBuilder("WorkflowProcessInstance").append(" [Id=").append(getId()).append(",processId=")
                .append(getProcessId()).append(",state=").append(getState()).append("]").toString();
    }

    @Override
    public void start() {
        start(null, null);
    }

    @Override
    public void start(String trigger, Object triggerData) {
        synchronized (this) {
            setStartDate(new Date());
            registerExternalEventNodeListeners();
            // activate timer event sub processes
            Node[] nodes = getNodeContainer().getNodes();
            for (Node node : nodes) {
                if (node instanceof EventSubProcessNode) {
                    Map<Timer, ProcessAction> timers = ((EventSubProcessNode) node).getTimers();
                    if (timers != null && !timers.isEmpty()) {
                        EventSubProcessNodeInstance eventSubProcess = (EventSubProcessNodeInstance) getNodeInstance(
                                node);
                        eventSubProcess.trigger(null,
                                io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
                    } else if (((EventSubProcessNode) node).findStartNode().hasCondition()) {
                        EventSubProcessNodeInstance eventSubProcess = (EventSubProcessNodeInstance) getNodeInstance(
                                node);
                        eventSubProcess.trigger(null,
                                io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
                    }
                }
            }
            super.start(trigger, triggerData);
        }
    }

    @Override
    public void configureSLA() {
        String slaDueDateExpression = (String) getProcess().getMetaData().get(CUSTOM_SLA_DUE_DATE);
        if (slaDueDateExpression != null) {
            TimerInstance timer = configureSLATimer(slaDueDateExpression);
            if (timer != null) {
                this.slaTimerId = timer.getId();
                this.slaDueDate = new Date(System.currentTimeMillis() + timer.getDelay());
                this.slaCompliance = SLA_PENDING;
                logger.debug("SLA for process instance {} is PENDING with due date {}", this.getId(), this.slaDueDate);
            }
        }
    }

    public TimerInstance configureSLATimer(String slaDueDateExpression) {
        // setup SLA if provided
        slaDueDateExpression = resolveVariable(slaDueDateExpression);
        if (slaDueDateExpression == null || slaDueDateExpression.trim().isEmpty()) {
            logger.debug("Sla due date expression resolved to no value '{}'", slaDueDateExpression);
            return null;
        }
        logger.debug("SLA due date is set to {}", slaDueDateExpression);
        long duration = DateTimeUtils.parseDuration(slaDueDateExpression);

        TimerInstance timerInstance = new TimerInstance();
        timerInstance.setTimerId(-1);
        timerInstance.setDelay(duration);
        timerInstance.setPeriod(0);
        if (useTimerSLATracking()) {
            ProcessInstanceJobDescription description = ProcessInstanceJobDescription.of(-1L,
                    DurationExpirationTime.after(duration), getId(), getProcessId(), getProcess().getVersion());
            timerInstance.setId(getProcessRuntime().getJobsService().scheduleProcessInstanceJob(description));
        }
        return timerInstance;
    }

    private void registerExternalEventNodeListeners() {
        for (Node node : getWorkflowProcess().getNodes()) {
            if (node instanceof EventNode && "external".equals(((EventNode) node).getScope())) {
                addEventListener(((EventNode) node).getType(), EMPTY_EVENT_LISTENER, true);
            } else if (node instanceof EventSubProcessNode) {
                List<String> events = ((EventSubProcessNode) node).getEvents();
                for (String type : events) {
                    addEventListener(type, EMPTY_EVENT_LISTENER, true);
                    if (isVariableExpression(type)) {
                        addEventListener(resolveVariable(type), EMPTY_EVENT_LISTENER, true);
                    }
                }
            }
        }
        if (getWorkflowProcess().getMetaData().containsKey(COMPENSATION)) {
            addEventListener("Compensation", new CompensationEventListener(this), true);
        }
    }

    private void unregisterExternalEventNodeListeners() {
        for (Node node : getWorkflowProcess().getNodes()) {
            if (node instanceof EventNode && "external".equals(((EventNode) node).getScope())) {
                externalEventListeners.remove(((EventNode) node).getType());
            }
        }
    }

    private void handleSLAViolation() {
        if (slaCompliance == SLA_PENDING) {
            InternalProcessRuntime processRuntime = getProcessRuntime();
            processRuntime.getProcessEventSupport().fireBeforeSLAViolated(this, processRuntime);
            logger.debug("SLA violated on process instance {}", getId());
            this.slaCompliance = SLA_VIOLATED;
            this.slaTimerId = null;
            processRuntime.getProcessEventSupport().fireAfterSLAViolated(this, processRuntime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void signalEvent(String type, Object event) {
        logger.debug("Signal {} received with data {} in process instance {}", type, event, getId());
        synchronized (this) {
            if (getState() != ProcessInstance.STATE_ACTIVE) {
                return;

            }

            if ("timerTriggered".equals(type)) {
                TimerInstance timer = (TimerInstance) event;
                if (timer.getId().equals(slaTimerId)) {
                    handleSLAViolation();
                    // no need to pass the event along as it was purely for SLA tracking
                    return;
                }
            }
            if ("slaViolation".equals(type)) {
                handleSLAViolation();
                // no need to pass the event along as it was purely for SLA tracking
                return;
            }

            List<NodeInstance> currentView = new ArrayList<>(this.nodeInstances);

            try {
                this.activatingNodeIds = new ArrayList<>();
                List<EventListener> listeners = eventListeners.get(type);
                if (listeners != null) {
                    for (EventListener listener : listeners) {
                        listener.signalEvent(type, event);
                    }
                }
                listeners = externalEventListeners.get(type);
                if (listeners != null) {
                    for (EventListener listener : listeners) {
                        listener.signalEvent(type, event);
                    }
                }
                if (!type.startsWith("Compensation")) { // exclude compensation events to avoid duplicated calls

                    for (Node node : getWorkflowProcess().getNodes()) {

                        if (node instanceof EventNodeInterface
                                && ((EventNodeInterface) node).acceptsEvent(type, event, getResolver(node, currentView))) {
                            if (node instanceof EventNode && ((EventNode) node).getFrom() == null) {
                                EventNodeInstance eventNodeInstance = (EventNodeInstance) getNodeInstance(node);
                                eventNodeInstance.signalEvent(type, event);
                            } else {
                                if (node instanceof EventSubProcessNode
                                        && (resolveVariables(((EventSubProcessNode) node).getEvents()).contains(type))) {
                                    EventSubProcessNodeInstance eventNodeInstance = (EventSubProcessNodeInstance) getNodeInstance(
                                            node);
                                    eventNodeInstance.signalEvent(type, event);
                                } else {
                                    List<NodeInstance> nodeInstances = getNodeInstances(node.getId(), currentView);
                                    if (nodeInstances != null && !nodeInstances.isEmpty()) {
                                        for (NodeInstance nodeInstance : nodeInstances) {
                                            ((EventNodeInstanceInterface) nodeInstance).signalEvent(type, event);
                                        }
                                    }
                                }
                            }
                        } else if (node instanceof StartNode && ((StartNode) node).getTriggers() != null) {
                            boolean accepted = ((StartNode) node).getTriggers().stream()
                                    .filter(EventTrigger.class::isInstance).anyMatch(t -> ((EventTrigger) t)
                                            .getEventFilters().stream().anyMatch(e -> e.acceptsEvent(type, event)));

                            if (accepted && node.getMetaData().get("acceptStartSignal") != null) {
                                StartNodeInstance startNodeInstance = (StartNodeInstance) getNodeInstance(node);
                                startNodeInstance.signalEvent(type, event);
                            }
                        }
                    }
                    if (((io.automatiko.engine.workflow.process.core.WorkflowProcess) getWorkflowProcess()).isDynamic()) {
                        for (Node node : getWorkflowProcess().getNodes()) {
                            if (node.hasMatchingEventListner(type) && node.getIncomingConnections().isEmpty()) {
                                NodeInstance nodeInstance = getNodeInstance(node);
                                if (nodeInstance != null) {

                                    if (event != null) {
                                        Map<String, Object> dynamicParams = new HashMap<>(getVariables());
                                        if (event instanceof Map) {
                                            dynamicParams.putAll((Map<String, Object>) event);
                                        } else if (event instanceof WorkflowProcessInstance) {
                                            // ignore variables of process instance type
                                        } else {
                                            dynamicParams.put("Data", event);
                                        }
                                        nodeInstance.setDynamicParameters(dynamicParams);
                                    }
                                    nodeInstance.trigger(null,
                                            io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
                                }
                            } else if (this instanceof ExecutableProcessInstance && node instanceof CompositeNode) {

                                Optional<NodeInstance> instance = this.nodeInstances.stream()
                                        .filter(ni -> ni.getNodeId() == node.getId()).findFirst();
                                instance.ifPresent(n -> ((CompositeNodeInstance) n).signalEvent(type, event));
                            }
                        }

                    }
                }
            } finally

            {
                if (this.activatingNodeIds != null) {
                    this.activatingNodeIds.clear();
                    this.activatingNodeIds = null;
                }
            }
        }

    }

    private Function<String, String> getResolver(Node node, List<NodeInstance> currentView) {
        if (node instanceof DynamicNode) {
            // special handling for dynamic node to allow to resolve variables from
            // individual node instances of the dynamic node
            // instead of just relying on process instance's variables
            return e -> {
                List<NodeInstance> nodeInstances = getNodeInstances(node.getId(), currentView);
                if (nodeInstances != null && !nodeInstances.isEmpty()) {
                    StringBuilder st = new StringBuilder();
                    for (NodeInstance ni : nodeInstances) {
                        String result = resolveVariable(e, new NodeInstanceResolverFactory(ni));
                        st.append(result).append("###");
                    }
                    return st.toString();
                } else {
                    return resolveVariable(e);
                }
            };
        } else {
            return this::resolveVariable;
        }
    }

    protected List<String> resolveVariables(List<String> events) {
        return events.stream().map(this::resolveVariable).collect(Collectors.toList());
    }

    private String resolveVariable(String s) {
        return resolveVariable(s, new ProcessInstanceResolverFactory(this));
    }

    private String resolveVariable(String s, VariableResolverFactory factory) {
        Map<String, String> replacements = new HashMap<>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacementKey = paramName;
            String defaultValue = null;
            if (paramName.contains(":")) {

                String[] items = paramName.split(":");
                paramName = items[0];
                defaultValue = items[1];
            }
            if (replacements.get(paramName) == null) {

                Object variableValue = getVariable(paramName);
                if (variableValue != null) {
                    replacements.put(replacementKey, variableValue.toString());
                } else {
                    try {
                        variableValue = MVEL.eval(paramName, factory);
                        String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                        replacements.put(replacementKey, variableValueString);
                    } catch (Throwable t) {
                        logger.error("Could not find variable scope for variable {}", paramName);
                    }
                }
            }
        }
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            s = s.replace("#{" + replacement.getKey() + "}", replacement.getValue());
        }
        return s;
    }

    @Override
    public void addEventListener(String type, EventListener listener, boolean external) {
        Map<String, List<EventListener>> eventListeners = external ? this.externalEventListeners : this.eventListeners;
        List<EventListener> listeners = eventListeners.computeIfAbsent(type, listenerType -> {
            final List<EventListener> newListenersList = new CopyOnWriteArrayList<>();
            if (external) {
                getProcessRuntime().getSignalManager().addEventListener(listenerType, this);
            }
            return newListenersList;
        });
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeEventListener(String type, EventListener listener, boolean external) {
        Map<String, List<EventListener>> eventListeners = external ? this.externalEventListeners : this.eventListeners;
        List<EventListener> listeners = eventListeners.get(type);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                eventListeners.remove(type);
                if (external) {
                    getProcessRuntime().getSignalManager().removeEventListener(type, this);
                }
            }
        } else {
            eventListeners.remove(type);
        }
    }

    private void removeEventListeners() {
        for (String type : externalEventListeners.keySet()) {
            getProcessRuntime().getSignalManager().removeEventListener(type, this);
        }
    }

    @Override
    public String[] getEventTypes() {
        return externalEventListeners.keySet().stream().map(this::resolveVariable).collect(Collectors.toList())
                .toArray(new String[externalEventListeners.size()]);
    }

    @Override
    public Set<EventDescription<?>> getEventDescriptions() {
        if (getState() == ProcessInstance.STATE_COMPLETED || getState() == ProcessInstance.STATE_ABORTED) {
            return Collections.emptySet();
        }
        VariableScope variableScope = (VariableScope) ((ContextContainer) getProcess())
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);
        Set<EventDescription<?>> eventDesciptions = new LinkedHashSet<>();

        List<EventListener> activeListeners = eventListeners.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());

        activeListeners
                .addAll(externalEventListeners.values().stream().flatMap(List::stream).collect(Collectors.toList()));

        activeListeners.forEach(el -> eventDesciptions.addAll(el.getEventDescriptions()));

        ((io.automatiko.engine.workflow.process.core.WorkflowProcess) getProcess()).getNodesRecursively().stream()
                .filter(n -> n instanceof EventNodeInterface).forEach(n -> {

                    NamedDataType dataType = null;
                    if (((EventNodeInterface) n).getVariableName() != null) {
                        Map<String, Object> dataOutputs = (Map<String, Object>) n.getMetaData().get("DataOutputs");
                        if (dataOutputs != null) {
                            for (Entry<String, Object> dOut : dataOutputs.entrySet()) {
                                dataType = new NamedDataType(dOut.getKey(), dOut.getValue());
                            }
                        } else {

                            Variable eventVar = variableScope.findVariable(((EventNodeInterface) n).getVariableName());
                            if (eventVar != null) {
                                dataType = new NamedDataType(eventVar.getName(), eventVar.getType());
                            }
                        }
                    }
                    if (n instanceof BoundaryEventNode) {
                        BoundaryEventNode boundaryEventNode = (BoundaryEventNode) n;
                        StateBasedNodeInstance attachedToNodeInstance = (StateBasedNodeInstance) getNodeInstances(true)
                                .stream().filter(ni -> ni.getNode().getMetaData().get(UNIQUE_ID)
                                        .equals(boundaryEventNode.getAttachedToNodeId()))
                                .findFirst().orElse(null);
                        if (attachedToNodeInstance != null) {
                            Map<String, String> properties = new HashMap<>();
                            properties.put("AttachedToID", attachedToNodeInstance.getNodeDefinitionId());
                            properties.put("AttachedToName", attachedToNodeInstance.getNodeName());
                            String eventType = EVENT_TYPE_SIGNAL;
                            String eventName = boundaryEventNode.getType();
                            Map<String, String> timerProperties = attachedToNodeInstance.extractTimerEventInformation();
                            if (timerProperties != null) {
                                properties.putAll(timerProperties);
                                eventType = "timer";
                                eventName = "timerTriggered";
                            }

                            eventDesciptions
                                    .add(new BaseEventDescription(eventName, (String) n.getMetaData().get(UNIQUE_ID),
                                            n.getName(), eventType, null, getId(), dataType, properties));

                        }

                    } else if (n instanceof EventSubProcessNode) {
                        EventSubProcessNode eventSubProcessNode = (EventSubProcessNode) n;
                        boolean isContainerActive = false;

                        if (eventSubProcessNode.getParentContainer() instanceof WorkflowProcess) {
                            isContainerActive = true;
                        } else if (eventSubProcessNode.getParentContainer() instanceof CompositeNode) {
                            isContainerActive = !getNodeInstances(
                                    ((CompositeNode) eventSubProcessNode.getParentContainer()).getId()).isEmpty();
                        }

                        if (isContainerActive) {
                            Node startNode = eventSubProcessNode.findStartNode();
                            Map<Timer, ProcessAction> timers = eventSubProcessNode.getTimers();
                            if (timers != null && !timers.isEmpty()) {
                                getNodeInstances(eventSubProcessNode.getId()).forEach(ni -> {

                                    Map<String, String> timerProperties = ((StateBasedNodeInstance) ni)
                                            .extractTimerEventInformation();
                                    if (timerProperties != null) {

                                        eventDesciptions.add(new BaseEventDescription("timerTriggered",
                                                (String) startNode.getMetaData().get("UniqueId"), startNode.getName(), "timer",
                                                ni.getId(), getId(), null, timerProperties));

                                    }
                                });
                            } else {

                                for (String eventName : eventSubProcessNode.getEvents()) {

                                    eventDesciptions.add(new BaseEventDescription(eventName,
                                            (String) startNode.getMetaData().get("UniqueId"), startNode.getName(), "signal",
                                            null, getId(), dataType));
                                }

                            }
                        }
                    } else if (n instanceof EventNode) {
                        NamedDataType finalDataType = dataType;
                        getNodeInstances(n.getId())
                                .forEach(ni -> eventDesciptions.add(new BaseEventDescription(((EventNode) n).getType(),
                                        (String) n.getMetaData().get(UNIQUE_ID), n.getName(),
                                        (String) n.getMetaData().getOrDefault(EVENT_TYPE, EVENT_TYPE_SIGNAL),
                                        ni.getId(), getId(), finalDataType)));
                    } else if (n instanceof StateNode) {
                        getNodeInstances(n.getId()).forEach(ni -> eventDesciptions.add(new BaseEventDescription(
                                (String) n.getMetaData().get(CONDITION), (String) n.getMetaData().get(UNIQUE_ID),
                                n.getName(), (String) n.getMetaData().getOrDefault(EVENT_TYPE, EVENT_TYPE_SIGNAL),
                                ni.getId(), getId(), null)));
                    }

                });

        return eventDesciptions;
    }

    @Override
    public void nodeInstanceCompleted(NodeInstance nodeInstance, String outType) {
        Node nodeInstanceNode = nodeInstance.getNode();
        if (nodeInstanceNode != null) {
            Object compensationBoolObj = nodeInstanceNode.getMetaData().get(IS_FOR_COMPENSATION);
            boolean isForCompensation = compensationBoolObj != null && (Boolean) compensationBoolObj;
            if (isForCompensation) {
                return;
            }
        }
        if (nodeInstance instanceof FaultNodeInstance || nodeInstance instanceof EndNodeInstance
                || ((io.automatiko.engine.workflow.process.core.WorkflowProcess) getWorkflowProcess()).isDynamic()
                || nodeInstance instanceof CompositeNodeInstance) {
            if (((io.automatiko.engine.workflow.process.core.WorkflowProcess) getProcess()).isAutoComplete()
                    && canComplete()) {
                setState(ProcessInstance.STATE_COMPLETED);
            }
        } else {
            throw new IllegalArgumentException(
                    "Completing a node instance that has no outgoing connection is not supported.");
        }
    }

    private boolean canComplete() {
        if (nodeInstances.isEmpty()) {
            for (Node node : getNodeContainer().getNodes()) {

                // check if there are any required nodes not yet completed
                if (node.getMetaData().containsKey("requiredRule")
                        && !getCompletedNodeIds().contains(node.getMetaData().get("UniqueId"))) {
                    return false;
                }
            }

            return true;
        } else {
            int eventSubprocessCounter = 0;
            for (NodeInstance nodeInstance : nodeInstances) {
                Node node = nodeInstance.getNode();
                if (node instanceof EventSubProcessNode) {
                    if (((EventSubProcessNodeInstance) nodeInstance).getNodeInstances().isEmpty()) {
                        eventSubprocessCounter++;
                    }
                } else {
                    return false;
                }
            }
            return eventSubprocessCounter == nodeInstances.size();
        }
    }

    public void addCompletedNodeId(String uniqueId) {
        this.completedNodeIds.add(uniqueId.intern());
    }

    public List<String> getCompletedNodeIds() {
        return new ArrayList<>(this.completedNodeIds);
    }

    @Override
    public int getCurrentLevel() {
        return currentLevel;
    }

    @Override
    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Map<String, Integer> getIterationLevels() {
        return iterationLevels;
    }

    public void addActivatingNodeId(String uniqueId) {
        if (this.activatingNodeIds == null) {
            return;
        }
        this.activatingNodeIds.add(uniqueId.intern());
    }

    public List<String> getActivatingNodeIds() {
        if (this.activatingNodeIds == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(this.activatingNodeIds);
    }

    @Override
    public Object getFaultData() {
        return faultData;
    }

    @Override
    public boolean isSignalCompletion() {
        return signalCompletion;
    }

    @Override
    public void setSignalCompletion(boolean signalCompletion) {
        this.signalCompletion = signalCompletion;
    }

    public String getCorrelationKey() {
        if (correlationKey == null && getMetaData().get(CORRELATION_KEY) != null) {
            this.correlationKey = ((CorrelationKey) getMetaData().get(CORRELATION_KEY)).toExternalForm();
        }
        return correlationKey;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public void setStartDate(Date startDate) {
        if (this.startDate == null) {
            this.startDate = startDate;
        }
    }

    protected boolean hasDeploymentId() {
        return this.deploymentId != null && !this.deploymentId.isEmpty();
    }

    protected boolean useAsync(final Node node) {
        if (!(node instanceof EventSubProcessNode)
                && (node instanceof ActionNode || node instanceof StateBasedNode || node instanceof EndNode)) {
            boolean asyncMode = Boolean.parseBoolean((String) node.getMetaData().get(CUSTOM_ASYNC));
            if (asyncMode) {
                return true;
            }
        }

        return false;
    }

    protected boolean useTimerSLATracking() {

        return true;
    }

    @Override
    public int getSlaCompliance() {
        return slaCompliance;
    }

    public void internalSetSlaCompliance(int slaCompliance) {
        this.slaCompliance = slaCompliance;
    }

    @Override
    public Date getSlaDueDate() {
        return slaDueDate;
    }

    public void internalSetSlaDueDate(Date slaDueDate) {
        this.slaDueDate = slaDueDate;
    }

    public String getSlaTimerId() {
        return slaTimerId;
    }

    public void internalSetSlaTimerId(String slaTimerId) {
        this.slaTimerId = slaTimerId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String getReferenceId() {
        return this.referenceId;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    @Override
    public void setInitiator(String initiator) {
        // set only once to avoid overriding initiator
        if (this.initiator == null) {
            this.initiator = initiator;
        }
    }

    @Override
    public String getReferenceFromRoot() {
        return referenceFromRoot;
    }

    @Override
    public void setReferenceFromRoot(String referenceFromRoot) {
        if (this.referenceFromRoot != null) {
            return;
        }
        if (referenceFromRoot != null) {
            String parentProcessInstanceId = getParentProcessInstanceId();
            if (parentProcessInstanceId != null && !parentProcessInstanceId.isEmpty()) {
                parentProcessInstanceId += ":";
            } else {
                parentProcessInstanceId = "";
            }
            this.referenceFromRoot = referenceFromRoot + getProcessId() + "/" + parentProcessInstanceId + getId() + "/";
        } else {
            this.referenceFromRoot = version() + getProcessId() + "/" + getId() + "/";
        }
    }

    protected String version() {
        String version = getProcess().getVersion();
        if (version != null && !version.trim().isEmpty()) {
            return "v" + version.replaceAll("\\.", "_") + "/";
        }
        return "";
    }

    public void internalSetReferenceFromRoot(String referenceFromRoot) {
        this.referenceFromRoot = referenceFromRoot;
    }

    private boolean isVariableExpression(String eventType) {
        if (eventType == null) {
            return false;
        }
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(eventType);
        return matcher.find();
    }

    @Override
    public String setErrorState(NodeInstance nodeInstanceInError, Exception e) {
        String errorId = UUID.randomUUID().toString();
        this.nodeIdInError = nodeInstanceInError.getNodeDefinitionId();
        Throwable rootException = getRootException(e);
        String errorDetails = null;
        if (e instanceof WorkItemExecutionError) {
            errorDetails = ((WorkItemExecutionError) e).getErrorDetails();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorDetails = sw.toString();
        }
        errors.add(new ExecutionsErrorInfo(nodeInstanceInError.getNodeDefinitionId(), errorId, rootException.getMessage(),
                errorDetails));
        setState(STATE_ERROR);
        logger.error("Unexpected error (id {}) while executing node {} in process instance {}", errorId,
                nodeInstanceInError.getNode().getName(), this.getId(), e);
        // remove node instance that caused an error
        ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) nodeInstanceInError
                .getNodeInstanceContainer()).removeNodeInstance(nodeInstanceInError);

        return errorId;
    }

    @Override
    public Collection<AdHocFragment> adHocFragments() {
        return Stream.of(getNodeContainer().getNodes())
                .filter(n -> !(n instanceof StartNode) && !(n instanceof BoundaryEventNode))
                .filter(n -> n.getIncomingConnections().isEmpty())
                .map(node -> new AdHocFragment.Builder(node.getClass()).withName(node.getName())
                        .withAutoStart(
                                Boolean.parseBoolean((String) node.getMetaData().get(Metadata.CUSTOM_AUTO_START)))
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<Milestone> milestones() {
        return getNodesByType(MilestoneNode.class).map(n -> {
            String uid = (String) n.getMetaData().get(UNIQUE_ID);
            return Milestone.builder().withId(uid).withName(n.getName()).withStatus(getMilestoneStatus(uid)).build();
        }).collect(Collectors.toSet());
    }

    private <N extends Node> Stream<N> getNodesByType(Class<N> nodeClass) {
        return getWorkflowProcess().getNodesRecursively().stream().filter(nodeClass::isInstance).map(nodeClass::cast);
    }

    private ItemDescription.Status getMilestoneStatus(String uid) {
        if (getCompletedNodeIds().contains(uid)) {
            return COMPLETED;
        }
        if (getActiveNodeIds().contains(uid)) {
            return ACTIVE;
        }
        return AVAILABLE;
    }

    protected Throwable getRootException(Throwable exception) {
        Throwable rootException = exception;
        while (rootException.getCause() != null) {
            rootException = rootException.getCause();
        }
        return rootException;
    }

    public void evaluateExpressionConditions() {

    }

    public boolean hasNodeInstanceActive(String nodeId) {
        boolean isActive = getActivatingNodeIds().contains(nodeId);

        if (!isActive) {
            isActive = getNodeInstances(true).stream()
                    .anyMatch(ni -> nodeId.equals(ni.getNode().getMetaData().get("UniqueId")));
        }

        return isActive;
    }

    public boolean multipleInstancesOfNodeAllowed(Node node) {
        boolean allowed = true;
        String uniqueNodeId = (String) node.getMetaData().get("UniqueId");
        if (node.getMetaData().getOrDefault("customAllowRepeat", "true").equals("false")
                && (hasNodeInstanceActive(uniqueNodeId) || getCompletedNodeIds().contains(uniqueNodeId))) {
            allowed = false;
        }

        return allowed;

    }

    protected void broadcaseNodeInstanceStateChange(NodeInstance nodeInstance) {

    }

    @Override
    public Collection<Tag> getTags() {
        if (tags == null || tags.isEmpty()) {
            evaluateTags();
        }
        return tags;
    }

    public Collection<Tag> evaluateTags() {
        if (this.tags == null) {
            this.tags = new LinkedHashSet<Tag>();
        }
        Collection<Tag> evaluatedTags = new LinkedHashSet<Tag>();

        Collection<TagDefinition> tagDefinitions = ((Process) getProcess()).getTagDefinitions();

        for (TagDefinition def : tagDefinitions) {

            String tag = def.get(getVariables());
            if (tag != null) {
                Tag tagInstance = new TagInstance(def.getId(), tag);
                evaluatedTags.add(tagInstance);

                this.tags.remove(tagInstance);
            }
        }
        // append all remaining tasks that didn't have definition - added manually on the instance
        evaluatedTags.addAll(this.tags);
        // replace existing ones
        this.tags = evaluatedTags;
        return evaluatedTags;
    }

    public void addTag(String value) {
        internalAddTag(value, value);
    }

    public void internalAddTag(String id, String value) {
        this.tags.add(new TagInstance(id, value));
    }

    public boolean removedTag(String id) {
        return this.tags.remove(new TagInstance(id, null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowProcessInstanceImpl)) {
            return false;
        }
        WorkflowProcessInstanceImpl that = (WorkflowProcessInstanceImpl) o;
        return getId().equals(that.getId()) && getState() == that.getState();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getState());
    }

    public void resetErrorForNode(String nodeInError) {
        this.errors.stream().filter(e -> e.getFailedNodeId().equals(nodeInError)).findFirst()
                .ifPresent(e -> this.errors.remove(e));
    }

    public List<ExecutionsErrorInfo> errors() {
        return this.errors;
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    public void internalSetExecutionErrors(List<ExecutionsErrorInfo> errors) {
        this.errors = errors;
    }
}
