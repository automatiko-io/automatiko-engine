
package io.automatiko.engine.workflow.process.instance.node;

import static io.automatiko.engine.workflow.base.core.context.variable.VariableScope.VARIABLE_SCOPE;
import static io.automatiko.engine.workflow.process.instance.impl.DummyEventListener.EMPTY_EVENT_LISTENER;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.mvel2.MVEL;

import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceState;
import io.automatiko.engine.api.workflow.BaseEventDescription;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.api.workflow.NamedDataType;
import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.services.time.TimerInstance;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.event.EventTransformer;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatiko.engine.workflow.base.instance.impl.AssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;
import io.automatiko.engine.workflow.process.instance.impl.ExtendedNodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceResolverFactory;
import io.automatiko.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;
import io.automatiko.engine.workflow.util.PatternConstants;

/**
 * Runtime counterpart of an event node.
 * 
 */
public class EventNodeInstance extends ExtendedNodeInstanceImpl
        implements EventListener, EventNodeInstanceInterface, EventBasedNodeInstanceInterface {

    private static final long serialVersionUID = 510l;

    public void signalEvent(String type, Object event) {
        if ("timerTriggered".equals(type)) {
            TimerInstance timerInstance = (TimerInstance) event;
            if (timerInstance.getId().equals(slaTimerId)) {
                handleSLAViolation();
            }
        } else if (("slaViolation:" + getId()).equals(type)) {

            handleSLAViolation();

        } else {
            String variableName = getEventNode().getVariableName();
            if (!getEventNode().getOutAssociations().isEmpty()) {
                for (DataAssociation association : getEventNode().getOutAssociations()) {

                    if (association.getAssignments() == null || association.getAssignments().isEmpty()) {
                        VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VARIABLE_SCOPE, association.getTarget());
                        if (variableScopeInstance != null) {
                            Variable varDef = variableScopeInstance.getVariableScope()
                                    .findVariable(association.getTarget());
                            DataType dataType = varDef.getType();
                            // exclude java.lang.Object as it is considered unknown type
                            if (!dataType.getStringType().endsWith("java.lang.Object")
                                    && !dataType.getStringType().endsWith("Object") && event instanceof String) {
                                event = dataType.readValue((String) event);
                            } else {
                                variableScopeInstance.getVariableScope().validateVariable(
                                        getProcessInstance().getProcessName(), association.getTarget(), event);
                            }
                            variableScopeInstance.setVariable(this, association.getTarget(), event);
                        } else {
                            String output = association.getSources().get(0);
                            String target = association.getTarget();

                            Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(target);
                            if (matcher.find()) {
                                String paramName = matcher.group(1);

                                String expression = VariableUtil.transformDotNotation(paramName, output);
                                NodeInstanceResolverFactory resolver = new NodeInstanceResolverFactory(this);
                                resolver.addExtraParameters(
                                        Collections.singletonMap(association.getSources().get(0), event));
                                Serializable compiled = MVEL.compileExpression(expression);
                                MVEL.executeExpression(compiled, resolver);
                                String varName = VariableUtil.nameFromDotNotation(paramName);
                                variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                        VARIABLE_SCOPE, varName);
                                variableScopeInstance.setVariable(this, varName, variableScopeInstance.getVariable(varName));
                            } else {
                                logger.warn("Could not find variable scope for variable {}", association.getTarget());
                                logger.warn("when trying to complete start node {}", getEventNode().getName());
                                logger.warn("Continuing without setting variable.");
                            }
                        }

                    } else {
                        Object data = event;
                        association.getAssignments().stream()
                                .forEach(assignment -> handleAssignment(assignment, data));
                    }
                }
            } else if (variableName != null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, variableName);
                if (variableScopeInstance != null) {

                    EventTransformer transformer = getEventNode().getEventTransformer();
                    if (transformer != null) {
                        event = transformer.transformEvent(event);
                    }
                    variableScopeInstance.setVariable(this, variableName, event);
                } else {
                    String output = "event";

                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(variableName);
                    if (matcher.find()) {
                        String paramName = matcher.group(1);

                        String expression = VariableUtil.transformDotNotation(paramName, output);
                        NodeInstanceResolverFactory resolver = new NodeInstanceResolverFactory(this);
                        resolver.addExtraParameters(Collections.singletonMap("event", event));
                        Serializable compiled = MVEL.compileExpression(expression);
                        MVEL.executeExpression(compiled, resolver);
                        String varName = VariableUtil.nameFromDotNotation(paramName);
                        variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                                VARIABLE_SCOPE, varName);
                        variableScopeInstance.setVariable(this, varName, variableScopeInstance.getVariable(varName));
                    } else {
                        logger.warn("Could not find variable scope for variable {}", variableName);
                        logger.warn("when trying to complete start node {}", getEventNode().getName());
                        logger.warn("Continuing without setting variable.");
                    }
                }
            }
            triggerCompleted();
        }
    }

    public void internalTrigger(final NodeInstance from, String type) {
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("An EventNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        addEventListeners();
        // Do nothing, event activated
    }

    protected void configureSla() {
        String slaDueDateExpression = (String) getNode().getMetaData().get("customSLADueDate");
        if (slaDueDateExpression != null) {
            TimerInstance timer = ((WorkflowProcessInstanceImpl) getProcessInstance())
                    .configureSLATimer(slaDueDateExpression);
            if (timer != null) {
                this.slaTimerId = timer.getId();
                this.slaDueDate = new Date(System.currentTimeMillis() + timer.getDelay());
                this.slaCompliance = ProcessInstance.SLA_PENDING;
                logger.debug("SLA for node instance {} is PENDING with due date {}", this.getId(), this.slaDueDate);
            }
        }
    }

    protected void handleSLAViolation() {
        if (slaCompliance == ProcessInstance.SLA_PENDING) {
            InternalProcessRuntime processRuntime = getProcessInstance().getProcessRuntime();
            processRuntime.getProcessEventSupport().fireBeforeSLAViolated(getProcessInstance(), this,
                    getProcessInstance().getProcessRuntime());
            logger.debug("SLA violated on node instance {}", getId());
            this.slaCompliance = ProcessInstance.SLA_VIOLATED;
            this.slaTimerId = null;
            processRuntime.getProcessEventSupport().fireAfterSLAViolated(getProcessInstance(), this,
                    getProcessInstance().getProcessRuntime());
        }
    }

    private void cancelSlaTimer() {
        if (this.slaTimerId != null && !this.slaTimerId.trim().isEmpty()) {
            JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
            jobService.cancelJob(this.slaTimerId);
            logger.debug("SLA Timer {} has been canceled", this.slaTimerId);
        }
    }

    protected void addTimerListener() {

        ((WorkflowProcessInstance) getProcessInstance()).addEventListener("timerTriggered",
                new VariableExternalEventListener("timerTriggered"), false);
        ((WorkflowProcessInstance) getProcessInstance()).addEventListener("timer",
                new VariableExternalEventListener("timer"), true);
        ((WorkflowProcessInstance) getProcessInstance()).addEventListener("slaViolation:" + getId(),
                new VariableExternalEventListener("slaViolation"), true);
    }

    public void removeTimerListeners() {
        ((WorkflowProcessInstance) getProcessInstance()).removeEventListener("timerTriggered",
                new VariableExternalEventListener("timerTriggered"), false);
        ((WorkflowProcessInstance) getProcessInstance()).removeEventListener("timer",
                new VariableExternalEventListener("timer"), true);
        ((WorkflowProcessInstance) getProcessInstance()).removeEventListener("slaViolation:" + getId(),
                new VariableExternalEventListener("slaViolation"), true);
    }

    public EventNode getEventNode() {
        return (EventNode) getNode();
    }

    public void triggerCompleted() {
        internalChangeState(NodeInstanceState.Occur);
        getProcessInstance().removeEventListener(getEventType(), getEventListener(), true);
        removeTimerListeners();
        if (this.slaCompliance == ProcessInstance.SLA_PENDING) {
            if (System.currentTimeMillis() > slaDueDate.getTime()) {
                // completion of the node instance is after expected SLA due date, mark it
                // accordingly
                this.slaCompliance = ProcessInstance.SLA_VIOLATED;
            } else {
                this.slaCompliance = ProcessInstance.STATE_COMPLETED;
            }
        }
        cancelSlaTimer();
        ((io.automatiko.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
                .setCurrentLevel(getLevel());
        triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, true);
    }

    @Override
    public void cancel() {
        getProcessInstance().removeEventListener(getEventType(), getEventListener(), true);
        removeTimerListeners();
        if (this.slaCompliance == ProcessInstance.SLA_PENDING) {
            if (System.currentTimeMillis() > slaDueDate.getTime()) {
                // completion of the process instance is after expected SLA due date, mark it
                // accordingly
                this.slaCompliance = ProcessInstance.SLA_VIOLATED;
            } else {
                this.slaCompliance = ProcessInstance.SLA_ABORTED;
            }
        }
        removeTimerListeners();
        super.cancel();
    }

    private class VariableExternalEventListener implements EventListener, Serializable {
        private static final long serialVersionUID = 5L;

        private String eventType;

        VariableExternalEventListener(String eventType) {
            this.eventType = eventType;
        }

        public String[] getEventTypes() {
            return new String[] { eventType };
        }

        public void signalEvent(String type, Object event) {
            callSignal(type, event);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            VariableExternalEventListener other = (VariableExternalEventListener) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (eventType == null) {
                if (other.eventType != null)
                    return false;
            } else if (!eventType.equals(other.eventType))
                return false;
            return true;
        }

        private EventNodeInstance getOuterType() {
            return EventNodeInstance.this;
        }
    }

    @Override
    public void addEventListeners() {
        String eventType = getEventType();
        if (isVariableExpression(getEventNode().getType())) {
            getProcessInstance().addEventListener(eventType, new VariableExternalEventListener(eventType), true);
        } else {
            getProcessInstance().addEventListener(eventType, getEventListener(), true);
        }
        if (this.slaTimerId != null && !this.slaTimerId.trim().isEmpty()) {
            addTimerListener();
        }
    }

    @Override
    public void removeEventListeners() {

    }

    public String getEventType() {
        return resolveVariable(getEventNode().getType());
    }

    protected EventListener getEventListener() {
        return EMPTY_EVENT_LISTENER;
    }

    private boolean isVariableExpression(String eventType) {
        if (eventType == null) {
            return false;
        }
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(eventType);
        if (matcher.find()) {
            return true;
        }

        return false;
    }

    private String resolveVariable(String s) {
        if (s == null) {
            return null;
        }

        Map<String, String> replacements = new HashMap<String, String>();
        Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(s);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacementKey = paramName;
            String defaultValue = "";
            if (paramName.contains(":")) {

                String[] items = paramName.split(":");
                paramName = items[0];
                defaultValue = items[1];
            }
            if (replacements.get(replacementKey) == null) {
                VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(
                        VariableScope.VARIABLE_SCOPE, paramName);
                if (variableScopeInstance != null) {
                    Object variableValue = variableScopeInstance.getVariable(paramName);
                    String variableValueString = variableValue == null ? defaultValue : variableValue.toString();
                    replacements.put(replacementKey, variableValueString);
                }
            }
        }
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            s = s.replace("#{" + replacement.getKey() + "}", replacement.getValue());
        }

        return s;
    }

    private void callSignal(String type, Object event) {
        signalEvent(type, event);
    }

    @Override
    public String[] getEventTypes() {
        return new String[] { getEventType() };
    }

    @Override
    public Set<EventDescription<?>> getEventDescriptions() {
        NamedDataType dataType = null;
        if (getEventNode().getVariableName() != null) {
            Map<String, Object> dataOutputs = (Map<String, Object>) getEventNode().getMetaData().get("DataOutputs");
            if (dataOutputs != null) {
                for (Entry<String, Object> dOut : dataOutputs.entrySet()) {
                    dataType = new NamedDataType(dOut.getKey(), dOut.getValue());
                }
            } else {
                VariableScope variableScope = (VariableScope) getEventNode().getContext(VariableScope.VARIABLE_SCOPE);
                Variable variable = variableScope.findVariable(getEventNode().getVariableName());
                dataType = new NamedDataType(variable.getName(), variable.getType());
            }

        }
        return Collections.singleton(new BaseEventDescription(getEventType(), getNodeDefinitionId(), getNodeName(),
                "signal", getId(), getProcessInstance().getId(), dataType));
    }

    private void handleAssignment(Assignment assignment, Object result) {
        AssignmentAction action = (AssignmentAction) assignment.getMetaData("Action");
        if (action == null) {
            return;
        }
        try {
            ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
            context.setNodeInstance(this);

            WorkItemImpl workItem = new WorkItemImpl();
            workItem.setResult("workflowdata", result);
            workItem.setResult("event", result);
            action.execute(workItem, context);
        } catch (WorkItemExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("unable to execute Assignment", e);
        }
    }
}
