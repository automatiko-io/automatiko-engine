package io.automatiko.engine.workflow.serverless.parser.core;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.ACTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatiko.engine.workflow.base.instance.impl.actions.ProcessInstanceCompensationAction;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.ConstraintImpl;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.Metadata;
import io.automatiko.engine.workflow.process.executable.core.ServerlessExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;
import io.automatiko.engine.workflow.serverless.parser.util.ServerlessWorkflowUtils;
import io.automatiko.engine.workflow.serverless.parser.util.WorkflowAppContext;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.exectimeout.ExecTimeout;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.retry.RetryDefinition;

public class ServerlessWorkflowFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerlessWorkflowFactory.class);

    public static final String DEFAULT_WORKFLOW_ID = "serverless";
    public static final String DEFAULT_WORKFLOW_NAME = "workflow";
    public static final String DEFAULT_PACKAGE_NAME = "io.automatiko.serverless";
    public static final String DEFAULT_VISIBILITY = "Public";
    public static final String DEFAULT_DECISION = "decision";
    public static final String JSON_NODE = "com.fasterxml.jackson.databind.JsonNode";
    public static final String DEFAULT_WORKFLOW_VAR = "workflowdata";
    public static final String UNIQUE_ID_PARAM = "UniqueId";
    public static final String EVENTBASED_PARAM = "EventBased";
    public static final String DEFAULT_SERVICE_IMPL = "Java";
    public static final String SERVICE_IMPL_KEY = "implementation";
    public static final String SERVICE_ENDPOINT = "endpoint";
    public static final String DEFAULT_HT_TASKNAME = "workflowhtask";
    public static final String DEFAULT_HT_SKIPPABLE = "true";
    public static final String HT_TASKNAME = "taskname";
    public static final String HT_SKIPPABLE = "skippable";
    public static final String HTP_GROUPID = "groupid";
    public static final String HT_ACTORID = "actorid";
    public static final String SERVICE_TASK_TYPE = "Service Task";
    public static final int DEFAULT_RETRY_LIMIT = 3;
    public static final int DEFAULT_RETRY_AFTER = 1000;

    private WorkflowAppContext workflowAppContext;

    public ServerlessWorkflowFactory(WorkflowAppContext workflowAppContext) {
        this.workflowAppContext = workflowAppContext;
    }

    public ExecutableProcess createProcess(Workflow workflow) {
        ExecutableProcess process = new ServerlessExecutableProcess();

        if (workflow.getId() != null && !workflow.getId().isEmpty()) {
            process.setId(workflow.getId());
        } else {
            LOGGER.info("setting default id {}", DEFAULT_WORKFLOW_ID);
            process.setId(DEFAULT_WORKFLOW_ID);
        }

        if (workflow.getName() != null && !workflow.getName().isEmpty()) {
            process.setName(workflow.getName());
        } else {
            LOGGER.info("setting default name {}", DEFAULT_WORKFLOW_NAME);
            process.setName(DEFAULT_WORKFLOW_NAME);
        }

        if (workflow.getVersion() != null && !workflow.getVersion().isEmpty()) {
            process.setVersion(workflow.getVersion());
        } else {
            LOGGER.info("no workflow version found.");
        }

        if (workflow.getMetadata() != null && workflow.getMetadata().get("package") != null) {
            process.setPackageName(workflow.getMetadata().get("package"));
        } else {
            process.setPackageName(DEFAULT_PACKAGE_NAME);
        }

        if (workflow.isKeepActive()) {
            process.setAutoComplete(false);
            process.setDynamic(true);
        } else {
            process.setAutoComplete(true);
        }
        process.setVisibility(DEFAULT_VISIBILITY);

        if (workflow.getMetadata() != null) {
            process.getMetaData().putAll(workflow.getMetadata());
        }

        return process;
    }

    public StartNode startNode(long id, String name, NodeContainer nodeContainer) {
        StartNode startNode = new StartNode();
        startNode.setId(id);
        startNode.setName(name);
        startNode.setInterrupting(true);

        nodeContainer.addNode(startNode);

        return startNode;
    }

    public StartNode messageStartNode(long id, EventDefinition eventDefinition, NodeContainer nodeContainer) {

        StartNode startNode = new StartNode();
        startNode.setId(id);
        startNode.setName(eventDefinition.getName());
        startNode.setInterrupting(true);

        startNode.setMetaData(Metadata.TRIGGER_MAPPING, DEFAULT_WORKFLOW_VAR);
        startNode.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");

        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
            startNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        }

        startNode.setMetaData(Metadata.TRIGGER_REF, eventDefinition.getSource());
        startNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);

        addOutMapping(startNode, "event", DEFAULT_WORKFLOW_VAR, null, null, null);

        addTriggerToStartNode(startNode, JSON_NODE);

        nodeContainer.addNode(startNode);

        return startNode;
    }

    public EndNode endNode(long id, String name, boolean terminate, NodeContainer nodeContainer) {
        EndNode endNode = new EndNode();
        endNode.setId(id);
        endNode.setName(name);
        endNode.setTerminate(terminate);

        nodeContainer.addNode(endNode);
        return endNode;
    }

    public EndNode compensateEndNode(long id, String name, NodeContainer nodeContainer) {
        EndNode endNode = new EndNode();
        endNode.setId(id);
        endNode.setName(name);

        endNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        endNode.setMetaData(Metadata.TRIGGER_TYPE, "Compensation");

        nodeContainer.addNode(endNode);
        return endNode;
    }

    public EndNode messageEndNode(long id, String name, Workflow workflow, End stateEnd, NodeContainer nodeContainer) {
        EndNode endNode = new EndNode();
        endNode.setTerminate(false);
        endNode.setId(id);
        endNode.setName(name);

        //currently support a single produce event
        if (!stateEnd.getProduceEvents().isEmpty()) {

            EventDefinition eventDef = ServerlessWorkflowUtils.getWorkflowEventFor(workflow,
                    stateEnd.getProduceEvents().get(0).getEventRef());

            endNode.setMetaData(Metadata.TRIGGER_REF, eventDef.getSource());
            endNode.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");

            if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()) != null) {
                endNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
                        ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()));
            }

            endNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);
            endNode.setMetaData(Metadata.MAPPING_VARIABLE, DEFAULT_WORKFLOW_VAR);
            addMessageEndNodeAction(endNode, DEFAULT_WORKFLOW_VAR, JSON_NODE);

            nodeContainer.addNode(endNode);
            return endNode;
        } else {
            LOGGER.error("Unable to find produce event definition for state end.");
            return null;
        }
    }

    public TimerNode timerNode(long id, String name, String delay, NodeContainer nodeContainer) {
        TimerNode timerNode = new TimerNode();
        timerNode.setId(id);
        timerNode.setName(name);
        timerNode.setMetaData(Metadata.EVENT_TYPE, "timer");

        Timer timer = new Timer();
        timer.setTimeType(Timer.TIME_DURATION);
        timer.setDelay(delay);
        timerNode.setTimer(timer);

        nodeContainer.addNode(timerNode);

        return timerNode;
    }

    public SubProcessNode callActivity(long id, String name, String calledId, boolean waitForCompletion,
            NodeContainer nodeContainer) {
        SubProcessNode subProcessNode = new SubProcessNode();
        subProcessNode.setId(id);
        subProcessNode.setName(name);
        subProcessNode.setProcessId(calledId);
        subProcessNode.setWaitForCompletion(waitForCompletion);
        subProcessNode.setIndependent(true);

        VariableScope variableScope = new VariableScope();
        subProcessNode.addContext(variableScope);
        subProcessNode.setDefaultContext(variableScope);

        Map<String, String> inputOtuputTypes = new HashMap<>();
        inputOtuputTypes.put(DEFAULT_WORKFLOW_VAR, JSON_NODE);
        subProcessNode.setMetaData("BPMN.InputTypes", inputOtuputTypes);
        subProcessNode.setMetaData("BPMN.OutputTypes", inputOtuputTypes);

        // parent and sub processes have process var "workflowdata"
        subProcessNode.addInMapping(DEFAULT_WORKFLOW_VAR, DEFAULT_WORKFLOW_VAR);
        subProcessNode.addOutMapping(DEFAULT_WORKFLOW_VAR, DEFAULT_WORKFLOW_VAR);

        nodeContainer.addNode(subProcessNode);

        return subProcessNode;
    }

    public void addMessageEndNodeAction(EndNode endNode, String variable, String messageType) {
        //        List<DroolsAction> actions = new ArrayList<>();
        //
        //        actions.add(new DroolsConsequenceAction("java",
        //                "org.drools.core.process.instance.impl.WorkItemImpl workItem = new org.drools.core.process.instance.impl.WorkItemImpl();" + EOL +
        //                        "workItem.setName(\"Send Task\");" + EOL +
        //                        "workItem.setNodeInstanceId(context.getNodeInstance().getId());" + EOL +
        //                        "workItem.setProcessInstanceId(context.getProcessInstance().getId());" + EOL +
        //                        "workItem.setNodeId(context.getNodeInstance().getNodeId());" + EOL +
        //                        "workItem.setParameter(\"MessageType\", \"" + messageType + "\");" + EOL +
        //                        (variable == null ? "" : "workItem.setParameter(\"Message\", " + variable + ");" + EOL) +
        //                        "workItem.setDeploymentId((String) context.getKnowledgeRuntime().getEnvironment().get(\"deploymentId\"));" + EOL +
        //                        "((org.drools.core.process.instance.WorkItemManager) context.getKnowledgeRuntime().getWorkItemManager()).internalExecuteWorkItem(workItem);"));
        //        endNode.setActions(ExtendedNodeImpl.EVENT_NODE_ENTER, actions);
    }

    public void addTriggerToStartNode(StartNode startNode, String triggerEventType) {
        EventTrigger trigger = new EventTrigger();
        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType(triggerEventType);
        trigger.addEventFilter(eventFilter);

        String mapping = (String) startNode.getMetaData(Metadata.TRIGGER_MAPPING);
        if (mapping != null) {
            trigger.addInMapping(mapping, startNode.getOutMapping(mapping));
        }

        startNode.addTrigger(trigger);
    }

    public void addOutMapping(StartNode startNode, String source, String target, String assignmentDialect,
            String assignmentFrom,
            String assignmentTo) {
        List<Assignment> assignments = null;
        if (assignmentFrom != null && assignmentTo != null) {
            assignments = Arrays.asList(new Assignment(assignmentDialect, assignmentFrom, assignmentTo));
        }
        DataAssociation dataAssociation = new DataAssociation(source, target, assignments, null);
        startNode.addOutAssociation(dataAssociation);
    }

    public ActionNode sendEventNode(long id, EventDefinition eventDefinition, NodeContainer nodeContainer) {
        ActionNode sendEventNode = new ActionNode();
        sendEventNode.setId(id);
        sendEventNode.setName(eventDefinition.getName());
        sendEventNode.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");
        sendEventNode.setMetaData(Metadata.MAPPING_VARIABLE, DEFAULT_WORKFLOW_VAR);
        sendEventNode.setMetaData(Metadata.TRIGGER_REF, eventDefinition.getSource());
        sendEventNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);

        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
            sendEventNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        }

        nodeContainer.addNode(sendEventNode);

        return sendEventNode;
    }

    public ActionNode compensationEventNode(long id, String name, NodeContainer nodeContainer, ExecutableProcess process) {
        ActionNode compensationEventNode = new ActionNode();
        compensationEventNode.setId(id);
        compensationEventNode.setName(name);

        compensationEventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        ProcessInstanceCompensationAction pic = new ProcessInstanceCompensationAction("implicit:" + process.getId());
        ProcessAction processAction = new ProcessAction();
        processAction.setMetaData(ACTION, pic);
        compensationEventNode.setAction(processAction);

        compensationEventNode.setMetaData(Metadata.TRIGGER_TYPE, "Compensation");
        compensationEventNode.setMetaData("NodeType", "IntermediateThrowEvent-None");
        compensationEventNode.setMetaData("CompensationEvent", "implicit:" + process.getId());

        nodeContainer.addNode(compensationEventNode);
        return compensationEventNode;
    }

    public EventNode consumeEventNode(long id, EventDefinition eventDefinition, NodeContainer nodeContainer) {
        EventNode eventNode = new EventNode();
        eventNode.setId(id);
        eventNode.setName(eventDefinition.getName());

        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType("Message-" + eventDefinition.getSource());
        eventNode.addEventFilter(eventFilter);
        eventNode.setVariableName(DEFAULT_WORKFLOW_VAR);
        eventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        eventNode.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");
        eventNode.setMetaData(Metadata.TRIGGER_REF, eventDefinition.getSource());
        eventNode.setMetaData(Metadata.EVENT_TYPE, "message");
        eventNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);

        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
            eventNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        }

        nodeContainer.addNode(eventNode);
        return eventNode;
    }

    public ActionNode scriptNode(long id, String name, String script, NodeContainer nodeContainer) {
        ActionNode scriptNode = new ActionNode();
        scriptNode.setId(id);
        scriptNode.setName(name);

        scriptNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        ProcessAction processAction = new ProcessAction();
        processAction.setMetaData(ACTION, script);
        scriptNode.setAction(processAction);

        scriptNode.setAction(new ConsequenceAction("java", script));

        nodeContainer.addNode(scriptNode);

        return scriptNode;
    }

    public WorkItemNode camelRouteServiceNode(long id, String name, FunctionDefinition function, NodeContainer nodeContainer) {
        WorkItemNode workItemNode = new WorkItemNode();
        workItemNode.setId(id);
        workItemNode.setName(name);
        workItemNode.setMetaData("Type", SERVICE_TASK_TYPE);

        Work work = new WorkImpl();
        workItemNode.setWork(work);

        work.setName("org.apache.camel.ProducerTemplate.requestBody");
        work.setParameter(SERVICE_ENDPOINT,
                ServerlessWorkflowUtils.resolveFunctionMetadata(function, SERVICE_ENDPOINT, workflowAppContext));
        work.setParameter("Interface", "org.apache.camel.ProducerTemplate");
        work.setParameter("Operation", "requestBody");
        work.setParameter("interfaceImplementationRef", "org.apache.camel.ProducerTemplate");

        String metaImpl = ServerlessWorkflowUtils.resolveFunctionMetadata(function, SERVICE_IMPL_KEY, workflowAppContext);
        if (metaImpl == null || metaImpl.isEmpty()) {
            metaImpl = DEFAULT_SERVICE_IMPL;
        }
        work.setParameter(SERVICE_IMPL_KEY, metaImpl);

        workItemNode.addInMapping("body", DEFAULT_WORKFLOW_VAR);
        workItemNode.addOutMapping("result", DEFAULT_WORKFLOW_VAR);

        nodeContainer.addNode(workItemNode);

        return workItemNode;
    }

    public WorkItemNode serviceNode(long id, Action action, FunctionDefinition function, NodeContainer nodeContainer) {

        String actionName = action.getName();
        String[] operationParts = function.getOperation().split("#");
        String interfaceStr = operationParts[0];
        String operationStr = operationParts[1];

        WorkItemNode workItemNode = new WorkItemNode();
        workItemNode.setId(id);
        workItemNode.setName(actionName);
        workItemNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        workItemNode.setMetaData("Type", SERVICE_TASK_TYPE);
        workItemNode.setMetaData("Implementation", "##WebService");

        Work work = new WorkImpl();
        workItemNode.setWork(work);
        work.setName(SERVICE_TASK_TYPE);

        work.setParameter("Interface", interfaceStr);
        work.setParameter("Operation", operationStr);
        work.setParameter("ParameterType", JSON_NODE);
        work.setParameter("interfaceImplementationRef", interfaceStr);
        work.setParameter("implementation", "##WebService");

        //work.setParameter("interfaceImplementationRef", ServerlessWorkflowUtils.resolveFunctionMetadata(function, SERVICE_INTERFACE_KEY, workflowAppContext));
        //work.setParameter("operationImplementationRef", ServerlessWorkflowUtils.resolveFunctionMetadata(function, SERVICE_OPERATION_KEY, workflowAppContext));
        //        String metaImpl = ServerlessWorkflowUtils.resolveFunctionMetadata(function, SERVICE_IMPL_KEY, workflowAppContext);
        //        if (metaImpl == null || metaImpl.isEmpty()) {
        //            metaImpl = DEFAULT_SERVICE_IMPL;
        //        }
        //        work.setParameter(SERVICE_IMPL_KEY, metaImpl);

        JsonNode params = action.getFunctionRef().getArguments();

        if (params != null) {
            Iterator<String> it = params.fieldNames();
            while (it.hasNext()) {

                String name = it.next();
                String value = params.get(name).asText();
                if (value.equals("$..*")) {
                    value = "workflowdata";
                }
                workItemNode.addInMapping("Parameter", value);
            }

        } else {
            workItemNode.addInMapping("Parameter", DEFAULT_WORKFLOW_VAR);
            workItemNode.addOutMapping("Result", DEFAULT_WORKFLOW_VAR);
        }

        nodeContainer.addNode(workItemNode);

        return workItemNode;

    }

    public void processVar(String varName, Class<?> varType, ExecutableProcess process) {
        Variable variable = new Variable();
        variable.setName(varName);
        variable.setType(new ObjectDataType(varType));
        process.getVariableScope().getVariables().add(variable);
    }

    public CompositeContextNode subProcessNode(long id, String name, NodeContainer nodeContainer) {
        CompositeContextNode subProcessNode = new CompositeContextNode();
        subProcessNode.setId(id);
        subProcessNode.setName(name);
        subProcessNode.setAutoComplete(true);
        subProcessNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(subProcessNode.getId()));

        nodeContainer.addNode(subProcessNode);

        return subProcessNode;
    }

    public Split splitNode(long id, String name, int type, NodeContainer nodeContainer) {
        Split split = new Split();
        split.setId(id);
        split.setName(name);
        split.setType(type);
        split.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));

        nodeContainer.addNode(split);
        return split;
    }

    public Split eventBasedSplit(long id, String name, NodeContainer nodeContainer) {
        Split split = new Split();
        split.setId(id);
        split.setName(name);
        split.setType(Split.TYPE_XAND);
        split.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        split.setMetaData(EVENTBASED_PARAM, "true");

        nodeContainer.addNode(split);
        return split;
    }

    public Join joinNode(long id, String name, int type, NodeContainer nodeContainer) {
        Join join = new Join();
        join.setId(id);
        join.setName(name);
        join.setType(type);
        join.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));

        nodeContainer.addNode(join);
        return join;
    }

    public ConstraintImpl splitConstraint(String name, String type, String dialect, String constraint, int priority,
            boolean isDefault) {
        ConstraintImpl constraintImpl = new ConstraintImpl();
        constraintImpl.setName(name);
        constraintImpl.setType(type);
        constraintImpl.setDialect(dialect);
        constraintImpl.setConstraint(constraint);
        constraintImpl.setPriority(priority);
        constraintImpl.setDefault(isDefault);

        return constraintImpl;
    }

    public HumanTaskNode humanTaskNode(long id, String name, FunctionDefinition function, ExecutableProcess process,
            NodeContainer nodeContainer) {
        // first add the node "decision" variable
        processVar(ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_TASKNAME, workflowAppContext)
                + DEFAULT_DECISION, JsonNode.class, process);
        // then the ht node
        HumanTaskNode humanTaskNode = new HumanTaskNode();
        humanTaskNode.setId(id);
        humanTaskNode.setName(name);
        Work work = new WorkImpl();
        work.setName("Human Task");
        humanTaskNode.setWork(work);

        work.setParameter("TaskName",
                ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_TASKNAME, workflowAppContext).length() > 0
                        ? ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_TASKNAME, workflowAppContext)
                        : DEFAULT_HT_TASKNAME);
        work.setParameter("Skippable",
                ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_SKIPPABLE, workflowAppContext).length() > 0
                        ? ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_SKIPPABLE, workflowAppContext)
                        : DEFAULT_HT_SKIPPABLE);

        if (ServerlessWorkflowUtils.resolveFunctionMetadata(function, HTP_GROUPID, workflowAppContext).length() > 0) {
            work.setParameter("GroupId",
                    ServerlessWorkflowUtils.resolveFunctionMetadata(function, HTP_GROUPID, workflowAppContext));
        }

        if (ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_ACTORID, workflowAppContext).length() > 0) {
            work.setParameter("ActorId",
                    ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_ACTORID, workflowAppContext));
        }
        work.setParameter("NodeName", name);

        humanTaskNode.addInMapping(DEFAULT_WORKFLOW_VAR, DEFAULT_WORKFLOW_VAR);
        humanTaskNode.addOutMapping(DEFAULT_DECISION, ServerlessWorkflowUtils.resolveFunctionMetadata(function, HT_TASKNAME,
                workflowAppContext) + DEFAULT_DECISION);

        nodeContainer.addNode(humanTaskNode);

        return humanTaskNode;
    }

    public RuleSetNode ruleSetNode(long id, String name, FunctionDefinition function, NodeContainer nodeContainer) {
        RuleSetNode ruleSetNode = new RuleSetNode();
        return ruleSetNode;
    }

    public BoundaryEventNode compensationBoundaryEventNode(long id, String name, ExecutableProcess process, Node attachToNode) {
        BoundaryEventNode boundaryEventNode = new BoundaryEventNode();
        boundaryEventNode.setId(id);
        boundaryEventNode.setName(name);

        EventTypeFilter filter = new EventTypeFilter();
        filter.setType("Compensation");
        boundaryEventNode.addEventFilter(filter);

        boundaryEventNode.setAttachedToNodeId(Long.toString(attachToNode.getId()));

        boundaryEventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        boundaryEventNode.setMetaData("EventType", "compensation");
        boundaryEventNode.setMetaData("AttachedTo", Long.toString(attachToNode.getId()));

        process.addNode(boundaryEventNode);
        return boundaryEventNode;
    }

    public BoundaryEventNode errorBoundaryEventNode(long id, Error error, ExecutableProcess process,
            CompositeContextNode embeddedSubProcess, Workflow workflow) {
        BoundaryEventNode boundaryEventNode = new BoundaryEventNode();

        boundaryEventNode.setId(id);
        boundaryEventNode.setName(error.getError());

        EventTypeFilter filter = new EventTypeFilter();
        filter.setType("Error-" + embeddedSubProcess.getId() + "-" + error.getCode());
        boundaryEventNode.addEventFilter(filter);

        boundaryEventNode.setAttachedToNodeId(Long.toString(embeddedSubProcess.getId()));

        boundaryEventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        boundaryEventNode.setMetaData("EventType", "error");
        boundaryEventNode.setMetaData("ErrorEvent", error.getCode());
        boundaryEventNode.setMetaData("AttachedTo", Long.toString(embeddedSubProcess.getId()));
        boundaryEventNode.setMetaData("HasErrorEvent", true);

        if (error.getRetryRef() != null && error.getRetryRef().length() > 0) {
            RetryDefinition retryDefinition = ServerlessWorkflowUtils.getWorkflowRetryFor(workflow, error.getRetryRef());

            int delayAsInt = ((Long) DateTimeUtils.parseDuration(retryDefinition.getDelay())).intValue();

            boundaryEventNode.setMetaData("ErrorRetry", retryDefinition.getDelay() == null ? DEFAULT_RETRY_AFTER : delayAsInt);
            boundaryEventNode.setMetaData("ErrorRetryLimit", retryDefinition.getMaxAttempts() == null ? DEFAULT_RETRY_LIMIT
                    : Integer.parseInt(retryDefinition.getMaxAttempts()));

            if (retryDefinition.getMultiplier() != null) {
                boundaryEventNode.setMetaData("ErrorRetryIncrementMultiplier",
                        Float.parseFloat(retryDefinition.getMultiplier()));
            }
        }

        process.addNode(boundaryEventNode);
        return boundaryEventNode;
    }

    public void connect(long fromId, long toId, String uniqueId, NodeContainer nodeContainer, boolean association) {
        Node from = nodeContainer.getNode(fromId);
        Node to = nodeContainer.getNode(toId);
        ConnectionImpl connection = new ConnectionImpl(
                from, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
                to, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        connection.setMetaData(UNIQUE_ID_PARAM, uniqueId);
        if (association) {
            connection.setMetaData("association", true);
        }
    }

    public void addExecutionTimeout(long id, ExecTimeout timeout, ExecutableProcess process) {
        process.setMetaData("timeout", timeout.getDuration());

        if (timeout.getRunBefore() != null) {
            List<Long> timeoutNodes = new ArrayList<>();
            for (Node node : process.getNodes()) {
                if (node.getName().equals(timeout.getRunBefore())) {
                    timeoutNodes.add(node.getId());

                    collectConnectedNodes(node, process, timeoutNodes);
                    break;
                }
            }
            process.setMetaData("timeoutNodes",
                    timeoutNodes.stream().map(l -> Long.toString(l)).collect(Collectors.joining(",")));
        }
    }

    public void validate(ExecutableProcess process) {
        ProcessValidationError[] errors = ExecutableProcessValidator.getInstance().validateProcess(process);
        for (ProcessValidationError error : errors) {
            LOGGER.error(error.toString());
        }
        if (errors.length > 0) {
            throw new RuntimeException("Workflow could not be validated !");
        }
    }

    private void collectConnectedNodes(Node start, NodeContainer container, List<Long> nodeIds) {
        List<Connection> outgoingConnections = start.getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE);
        if (outgoingConnections.isEmpty()) {
            return;
        }

        for (Connection conn : outgoingConnections) {

            nodeIds.add(conn.getTo().getId());

            collectConnectedNodes(conn.getTo(), container, nodeIds);
        }
    }

}
