package io.automatiko.engine.workflow.serverless.parser;

import static io.automatiko.engine.workflow.process.executable.core.Metadata.ACTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.FunctionTagDefinition;
import io.automatiko.engine.workflow.base.core.Process;
import io.automatiko.engine.workflow.base.core.StaticTagDefinition;
import io.automatiko.engine.workflow.base.core.TagDefinition;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.context.variable.JsonVariableScope;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.JsonNodeDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.base.core.impl.WorkImpl;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.core.validation.ProcessValidationError;
import io.automatiko.engine.workflow.base.instance.impl.actions.ProcessInstanceCompensationAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.OutputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskInputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskOutputJqAssignmentAction;
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
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.Metadata;
import io.automatiko.engine.workflow.process.executable.core.ServerlessExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.validation.ExecutableProcessValidator;
import io.automatiko.engine.workflow.sw.ServerlessFunctions;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.ErrorDefinition;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.events.OnEvents;
import io.serverlessworkflow.api.filters.ActionDataFilter;
import io.serverlessworkflow.api.filters.EventDataFilter;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.produce.ProduceEvent;
import io.serverlessworkflow.api.retry.RetryDefinition;
import io.serverlessworkflow.api.timeouts.WorkflowExecTimeout;
import io.serverlessworkflow.api.workflow.Constants;
import io.serverlessworkflow.utils.WorkflowUtils;

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
    public static final String SERVICE_TASK_TYPE = "Service Task";
    public static final int DEFAULT_RETRY_LIMIT = 3;
    public static final int DEFAULT_RETRY_AFTER = 1000;

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
        if (workflow.getDescription() != null) {
            process.setMetaData("Documentation", workflow.getDescription());
        }

        if (workflow.getConstants() != null) {
            Constants constants = workflow.getConstants();

            String value = constants.getConstantsDef().toString();
            Variable constantsVariable = new Variable("contantsVariable", "$CONST",
                    new JsonNodeDataType());
            constantsVariable.setMetaData("value", value.replaceAll("\"", "\\\""));
            process.getVariableScope().addVariable(constantsVariable);
        }

        if (workflow.getAnnotations() != null) {
            List<TagDefinition> tagDefinitions = new ArrayList<TagDefinition>();
            int counter = 0;
            for (String tag : workflow.getAnnotations()) {

                if (tag.startsWith("${")) {
                    tagDefinitions
                            .add(new FunctionTagDefinition(String.valueOf(++counter), unwrapExpression(tag),
                                    (exp, vars) -> {
                                        Object result = ServerlessFunctions.expression(vars, exp);
                                        if (result instanceof TextNode) {
                                            return ((TextNode) result).asText();
                                        }
                                        return result.toString();
                                    }));
                } else {
                    tagDefinitions.add(new StaticTagDefinition(String.valueOf(++counter), tag));
                }
            }
            ((Process) process).setTagDefinitions(tagDefinitions);
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

    public ActionNode injectStateNode(long id, String name, NodeContainer nodeContainer, String dataToInject) {
        ActionNode actionNode = new ActionNode();
        actionNode.setId(id);
        actionNode.setName(name);
        ConsequenceAction processAction = new ConsequenceAction(null,
                "inject(context, " + escapeExpression(dataToInject) + ");");

        io.automatiko.engine.workflow.base.instance.impl.Action injectAction = context -> {
            io.automatiko.engine.workflow.sw.ServerlessFunctions.inject(context, dataToInject);
        };

        processAction.setMetaData(ACTION, injectAction);

        actionNode.setAction(processAction);

        nodeContainer.addNode(actionNode);

        return actionNode;
    }

    public ActionNode stateDataFilterActionNode(long id, String name, NodeContainer nodeContainer, String outputFilterString) {
        ActionNode actionNode = new ActionNode();
        actionNode.setId(id);
        actionNode.setName(name);

        String outputFilter = unwrapExpression(outputFilterString);

        ConsequenceAction processAction = new ConsequenceAction(null,
                "new io.automatiko.engine.workflow.base.instance.impl.jq.OutputJqAssignmentAction("
                        + escapeExpression(outputFilter) + ").execute(null, context);");

        io.automatiko.engine.workflow.base.instance.impl.Action injectAction = context -> {

            new OutputJqAssignmentAction(outputFilter).execute(null, context);

        };

        processAction.setMetaData(ACTION, injectAction);

        actionNode.setAction(processAction);

        nodeContainer.addNode(actionNode);

        return actionNode;
    }

    public ActionNode expressionActionStateNode(long id, String name, NodeContainer nodeContainer, String expression,
            Action action) {
        ActionNode actionNode = new ActionNode();
        actionNode.setId(id);
        actionNode.setName(name);

        ActionDataFilter actionDataFilter = action.getActionDataFilter();

        StringBuilder functionArguments = new StringBuilder();
        if (action.getFunctionRef().getArguments() != null) {
            functionArguments.append("(");

            for (JsonNode argument : action.getFunctionRef().getArguments()) {
                functionArguments.append(unwrapExpression(argument.toString())).append(",");
            }
            functionArguments.deleteCharAt(functionArguments.length() - 1);

            functionArguments.append(")");
        }

        String inputFilter = actionDataFilter == null ? null : unwrapExpression(actionDataFilter.getFromStateData());
        String outputFilter = actionDataFilter == null ? null : unwrapExpression(actionDataFilter.getResults());
        String scopeFilter = actionDataFilter == null ? null : unwrapExpression(actionDataFilter.getToStateData());

        ConsequenceAction processAction = new ConsequenceAction(null,
                "expression(context, " + escapeExpression(expression + functionArguments) + ", " + escapeExpression(inputFilter)
                        + ");");

        if (actionDataFilter != null && actionDataFilter.isUseResults()) {
            processAction = new ConsequenceAction(null,
                    "expression(context, " + escapeExpression(expression + functionArguments) + ", "
                            + escapeExpression(inputFilter)
                            + ", " + escapeExpression(outputFilter) + ", " + escapeExpression(scopeFilter) + ");");
        }

        io.automatiko.engine.workflow.base.instance.impl.Action injectAction = context -> {
            if (actionDataFilter != null && actionDataFilter.isUseResults()) {
                io.automatiko.engine.workflow.sw.ServerlessFunctions.expression(context, expression, inputFilter,
                        outputFilter, scopeFilter);
            } else {

                io.automatiko.engine.workflow.sw.ServerlessFunctions.expression(context, expression, inputFilter);
            }
        };

        processAction.setMetaData(ACTION, injectAction);

        actionNode.setAction(processAction);

        nodeContainer.addNode(actionNode);

        return actionNode;
    }

    public StartNode messageStartNode(long id, EventDefinition eventDefinition, OnEvents onEvents,
            NodeContainer nodeContainer) {

        StartNode startNode = new StartNode();
        startNode.setId(id);
        startNode.setName(eventDefinition.getName());

        startNode.setMetaData(Metadata.TRIGGER_MAPPING, DEFAULT_WORKFLOW_VAR);
        startNode.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");

        //        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
        //            startNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
        //                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        //        }

        startNode.setMetaData(Metadata.TRIGGER_REF, eventDefinition.getName());
        startNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);

        if (eventDefinition.getCorrelation() != null && !eventDefinition.getCorrelation().isEmpty()) {

            startNode.setMetaData("TriggerCorrelationExpr",
                    "extensionAttribute(eventData, \"" + eventDefinition.getCorrelation().get(0).getContextAttributeName()
                            + "\")");
            startNode.setMetaData("acceptStartSignal", "true");
        }

        EventTrigger trigger = new EventTrigger();
        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType("Message-" + eventDefinition.getName());
        trigger.addEventFilter(eventFilter);

        String mapping = (String) startNode.getMetaData(Metadata.TRIGGER_MAPPING);
        if (mapping != null) {
            trigger.addInMapping(mapping, startNode.getOutMapping(mapping));
        }

        startNode.addTrigger(trigger);

        startNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));

        if (eventDefinition.getMetadata() != null) {
            eventDefinition.getMetadata().forEach((k, v) -> startNode.setMetaData(k, v));
        }

        //        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
        //            eventNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
        //                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        //        }

        boolean useData = true;
        String outputFilter = null;
        String scopeFilter = null;
        if (onEvents.getEventDataFilter() != null) {
            useData = onEvents.getEventDataFilter().isUseData();
            outputFilter = unwrapExpression(onEvents.getEventDataFilter().getData());
            scopeFilter = unwrapExpression(onEvents.getEventDataFilter().getToStateData());
        }

        if (useData) {
            Assignment outAssignment = new Assignment("jq", null, null);
            outAssignment.setMetaData("Action", new TaskOutputJqAssignmentAction(outputFilter, scopeFilter, true));
            startNode.addOutAssociation(
                    new DataAssociation(Collections.emptyList(), "", Arrays.asList(outAssignment), null));
        }

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

            EventDefinition eventDef = WorkflowUtils.getDefinedProducedEvents(workflow).stream().filter(e -> e.getName().equals(
                    stateEnd.getProduceEvents().get(0).getEventRef())).findFirst().get();

            endNode.setMetaData(Metadata.TRIGGER_REF, eventDef.getSource());
            endNode.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");

            //            if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()) != null) {
            //                endNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
            //                        ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()));
            //            }

            endNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);
            endNode.setMetaData(Metadata.MAPPING_VARIABLE, DEFAULT_WORKFLOW_VAR);

            nodeContainer.addNode(endNode);
            return endNode;
        } else {
            LOGGER.error("Unable to find produce event definition for state end.");
            return null;
        }
    }

    public ActionNode produceMessageNode(long id, String name, Workflow workflow, ProduceEvent event,
            NodeContainer nodeContainer) {
        ActionNode produceEventNode = new ActionNode();
        produceEventNode.setId(id);
        produceEventNode.setName(name);

        EventDefinition eventDef = WorkflowUtils.getDefinedProducedEvents(workflow).stream().filter(e -> e.getName().equals(
                event.getEventRef())).findFirst().get();

        produceEventNode.setMetaData(Metadata.TRIGGER_REF, eventDef.getType());
        produceEventNode.setMetaData(Metadata.TRIGGER_SOURCE, eventDef.getSource());
        produceEventNode.setMetaData(Metadata.TRIGGER_TYPE, "ProduceMessage");

        //            if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()) != null) {
        //                endNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
        //                        ServerlessWorkflowUtils.correlationExpressionFromSource(eventDef.getSource()));
        //            }

        produceEventNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);

        if (eventDef.getMetadata() != null && eventDef.getMetadata().containsKey(Metadata.CONNECTOR)) {
            produceEventNode.setMetaData(Metadata.CONNECTOR, eventDef.getMetadata().containsKey(Metadata.CONNECTOR));
        }

        if (event.getData() == null || event.getData().isEmpty()) {
            throw new IllegalArgumentException("Produce Event " + name + " does not have data set");
        }
        produceEventNode.setMetaData(Metadata.MAPPING_VARIABLE, event.getData());

        nodeContainer.addNode(produceEventNode);
        return produceEventNode;

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

        //        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
        //            sendEventNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
        //                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        //        }

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

    public EventNode consumeEventNode(long id, EventDefinition eventDefinition, EventDataFilter eventDataFilter,
            NodeContainer nodeContainer) {
        EventNode eventNode = new EventNode();
        eventNode.setId(id);
        eventNode.setName(eventDefinition.getName());

        EventTypeFilter eventFilter = new EventTypeFilter();
        eventFilter.setType("Message-" + eventDefinition.getName());
        eventNode.addEventFilter(eventFilter);

        eventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        eventNode.setMetaData(Metadata.TRIGGER_TYPE, "ConsumeMessage");
        eventNode.setMetaData(Metadata.TRIGGER_REF, eventDefinition.getName());
        eventNode.setMetaData(Metadata.EVENT_TYPE, "message");
        eventNode.setMetaData(Metadata.MESSAGE_TYPE, JSON_NODE);
        eventNode.setVariableName(DEFAULT_WORKFLOW_VAR);

        if (eventDefinition.getMetadata() != null) {
            eventDefinition.getMetadata().forEach((k, v) -> eventNode.setMetaData(k, v));
        }

        if (eventDefinition.getCorrelation() != null && !eventDefinition.getCorrelation().isEmpty()) {

            eventNode.setMetaData("TriggerCorrelationExpr",
                    "extensionAttribute(eventData, \"" + eventDefinition.getCorrelation().get(0).getContextAttributeName()
                            + "\")");
        }

        //        if (ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()) != null) {
        //            eventNode.setMetaData(Metadata.TRIGGER_CORRELATION_EXPR,
        //                    ServerlessWorkflowUtils.correlationExpressionFromSource(eventDefinition.getSource()));
        //        }

        boolean useData = true;
        String outputFilter = null;
        String scopeFilter = null;
        if (eventDataFilter != null) {
            useData = eventDataFilter.isUseData();
            outputFilter = unwrapExpression(eventDataFilter.getData());
            scopeFilter = unwrapExpression(eventDataFilter.getToStateData());
        }

        if (useData) {
            Assignment outAssignment = new Assignment("jq", null, null);
            outAssignment.setMetaData("Action", new TaskOutputJqAssignmentAction(outputFilter, scopeFilter, true));
            eventNode.addOutAssociation(
                    new DataAssociation(Collections.emptyList(), "", Arrays.asList(outAssignment), null));
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

        work.setParameter("interfaceImplementationRef", interfaceStr);
        work.setParameter("implementation", "##WebService");

        JsonNode params = action.getFunctionRef().getArguments();
        String inputFilter = null;
        String outputFilter = null;
        String scopeFilter = null;
        if (action.getActionDataFilter() != null) {
            inputFilter = unwrapExpression(action.getActionDataFilter().getFromStateData());
            outputFilter = unwrapExpression(action.getActionDataFilter().getResults());
            scopeFilter = unwrapExpression(action.getActionDataFilter().getToStateData());
        }
        Set<String> paramNames = new LinkedHashSet<>();

        if (params != null) {

            Iterator<String> it = params.fieldNames();
            while (it.hasNext()) {

                String name = it.next();
                String value = params.get(name).toString();
                work.setParameter(name, unwrapExpression(value));
                paramNames.add(name);

                work.addParameterDefinition(new ParameterDefinitionImpl(name, new JsonNodeDataType()));
            }
        } else {
            work.setParameter("ParameterType", JSON_NODE);
        }
        Assignment assignment = new Assignment("jq", null, null);
        assignment.setMetaData("Action", new TaskInputJqAssignmentAction(inputFilter, paramNames));
        workItemNode.addInAssociation(
                new DataAssociation(Collections.emptyList(), "", Arrays.asList(assignment), null));

        Assignment outAssignment = new Assignment("jq", null, null);
        outAssignment.setMetaData("Action", new TaskOutputJqAssignmentAction(outputFilter, scopeFilter));
        workItemNode.addOutAssociation(
                new DataAssociation(Collections.emptyList(), "", Arrays.asList(outAssignment), null));

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

        JsonVariableScope variableScope = new JsonVariableScope();
        subProcessNode.addContext(variableScope);
        subProcessNode.setDefaultContext(variableScope);

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

        // then the ht node
        HumanTaskNode humanTaskNode = new HumanTaskNode();
        humanTaskNode.setId(id);
        humanTaskNode.setName(name);
        Work work = new WorkImpl();
        work.setName("Human Task");
        humanTaskNode.setWork(work);

        work.setParameter("NodeName", name);

        humanTaskNode.addInMapping(DEFAULT_WORKFLOW_VAR, DEFAULT_WORKFLOW_VAR);

        nodeContainer.addNode(humanTaskNode);

        return humanTaskNode;
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

    public BoundaryEventNode errorBoundaryEventNode(long id, List<ErrorDefinition> defs, RetryDefinition retry,
            NodeContainer nodeContainer,
            Node attachedTo, Workflow workflow) {
        BoundaryEventNode boundaryEventNode = new BoundaryEventNode();

        boundaryEventNode.setId(id);
        boundaryEventNode.setName(defs.stream().map(def -> def.getName()).collect(Collectors.joining("|")));

        String errorCodes = defs.stream().map(def -> def.getCode()).collect(Collectors.joining(","));
        String attachedToId = (String) attachedTo.getMetaData().getOrDefault(UNIQUE_ID_PARAM,
                Long.toString(attachedTo.getId()));
        EventTypeFilter filter = new EventTypeFilter();
        filter.setType("Error-" + attachedToId + "-" + errorCodes);
        boundaryEventNode.addEventFilter(filter);

        boundaryEventNode.setAttachedToNodeId(attachedToId);

        boundaryEventNode.setMetaData(UNIQUE_ID_PARAM, Long.toString(id));
        boundaryEventNode.setMetaData("EventType", "error");
        boundaryEventNode.setMetaData("ErrorEvent", errorCodes);
        boundaryEventNode.setMetaData("AttachedTo", attachedToId);
        boundaryEventNode.setMetaData("HasErrorEvent", true);

        if (retry != null) {

            int delayAsInt = ((Long) DateTimeUtils.parseDuration(retry.getDelay())).intValue();

            boundaryEventNode.setMetaData("ErrorRetry", retry.getDelay() == null ? DEFAULT_RETRY_AFTER : delayAsInt);
            boundaryEventNode.setMetaData("ErrorRetryLimit", retry.getMaxAttempts() == null ? DEFAULT_RETRY_LIMIT
                    : Integer.parseInt(retry.getMaxAttempts()));

            if (retry.getMultiplier() != null) {
                boundaryEventNode.setMetaData("ErrorRetryIncrementMultiplier",
                        Float.parseFloat(retry.getMultiplier()));
            }
        }

        nodeContainer.addNode(boundaryEventNode);
        return boundaryEventNode;
    }

    public Connection connect(long fromId, long toId, String uniqueId, NodeContainer nodeContainer, boolean association) {
        Node from = nodeContainer.getNode(fromId);
        Node to = nodeContainer.getNode(toId);
        ConnectionImpl connection = new ConnectionImpl(
                from, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE,
                to, io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
        connection.setMetaData(UNIQUE_ID_PARAM, uniqueId);
        if (association) {
            connection.setMetaData("association", true);
        }

        return connection;
    }

    public void addExecutionTimeout(long id, WorkflowExecTimeout timeout, ExecutableProcess process) {
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

    public void collectConnectedNodes(Node start, NodeContainer container, List<Long> nodeIds) {
        List<Connection> outgoingConnections = start.getOutgoingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE);
        if (outgoingConnections.isEmpty()) {
            return;
        }

        for (Connection conn : outgoingConnections) {

            nodeIds.add(conn.getTo().getId());

            collectConnectedNodes(conn.getTo(), container, nodeIds);
        }
    }

    public String unwrapExpression(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (input.startsWith("${")) {
            return trimmed.trim().substring(2, trimmed.length() - 2);
        } else if (input.startsWith("\"${")) {
            return trimmed.trim().substring(3, trimmed.length() - 3);
        } else if (input.startsWith("\"")) {
            return trimmed.trim().substring(1, trimmed.length() - 1);
        }
        return input.trim();
    }

    protected String escapeExpression(String expression) {
        if (expression == null) {
            return null;
        }

        return "\"" + expression.replaceAll("\\\"", "\\\\\"") + "\"";
    }
}
