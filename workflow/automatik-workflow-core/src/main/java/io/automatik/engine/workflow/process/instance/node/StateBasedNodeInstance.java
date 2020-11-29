
package io.automatik.engine.workflow.process.instance.node;

import static io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.event.process.ContextAwareEventListener;
import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.ExactExpirationTime;
import io.automatik.engine.api.jobs.ExpirationTime;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.NodeInstanceState;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.context.variable.VariableScopeInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.ProcessAction;
import io.automatik.engine.workflow.process.core.impl.NodeImpl;
import io.automatik.engine.workflow.process.core.node.StateBasedNode;
import io.automatik.engine.workflow.process.instance.impl.ExtendedNodeInstanceImpl;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public abstract class StateBasedNodeInstance extends ExtendedNodeInstanceImpl
        implements EventBasedNodeInstanceInterface, EventListener {

    private static final long serialVersionUID = 510l;

    private static final Logger logger = LoggerFactory.getLogger(StateBasedNodeInstance.class);

    private List<String> timerInstances;

    public StateBasedNode getEventBasedNode() {
        return (StateBasedNode) getNode();
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        super.internalTrigger(from, type);
        // if node instance was cancelled, abort
        if (getNodeInstanceContainer().getNodeInstance(getId()) == null) {
            return;
        }
        addCompletionListeners();
        // activate timers
        Map<Timer, ProcessAction> timers = getEventBasedNode().getTimers();
        if (timers != null) {
            addTimerListener();
            timerInstances = new ArrayList<>(timers.size());
            JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
            for (Timer timer : timers.keySet()) {
                ExpirationTime expirationTime = createTimerInstance(timer);
                String jobId = jobService.scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(timer.getId(),
                        expirationTime, getProcessInstance().getId(), getProcessInstance().getRootProcessInstanceId(),
                        getProcessInstance().getProcessId(), getProcessInstance().getProcess().getVersion(),
                        getProcessInstance().getRootProcessId()));
                timerInstances.add(jobId);
            }
        }
        ((WorkflowProcessInstanceImpl) getProcessInstance())
                .addActivatingNodeId((String) getNode().getMetaData().get("UniqueId"));
        if (getExtendedNode().hasCondition()) {

            getProcessInstance().getProcessRuntime().addEventListener(ContextAwareEventListener.using(getId(), listener -> {
                ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
                context.setProcessInstance(getProcessInstance());
                context.setNodeInstance(this);
                if (getExtendedNode().isMet(context)) {
                    getProcessInstance().getProcessRuntime().removeEventListener(listener);
                    getProcessInstance().signalEvent((String) getExtendedNode().getMetaData("ConditionEventType"), null);

                }
            }));
        }
    }

    @Override
    protected void configureSla() {
        String slaDueDateExpression = (String) getNode().getMetaData().get("customSLADueDate");
        if (slaDueDateExpression != null) {
            TimerInstance timer = ((WorkflowProcessInstanceImpl) getProcessInstance())
                    .configureSLATimer(slaDueDateExpression);
            if (timer != null) {
                this.slaTimerId = timer.getId();
                this.slaDueDate = new Date(System.currentTimeMillis() + timer.getDelay());
                this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.SLA_PENDING;
                logger.debug("SLA for node instance {} is PENDING with due date {}", this.getId(), this.slaDueDate);
                addTimerListener();
            }
        }
    }

    protected ExpirationTime createTimerInstance(Timer timer) {

        return configureTimerInstance(timer);

    }

    protected ExpirationTime configureTimerInstance(Timer timer) {
        String s = null;
        long duration = -1;
        switch (timer.getTimeType()) {
            case Timer.TIME_CYCLE:
                if (timer.getPeriod() != null) {

                    long actualDelay = DateTimeUtils.parseDuration(resolveVariable(timer.getDelay()));
                    if (timer.getPeriod() == null) {
                        return DurationExpirationTime.repeat(actualDelay, actualDelay, Integer.MAX_VALUE);
                    } else {
                        return DurationExpirationTime.repeat(actualDelay,
                                DateTimeUtils.parseDuration(resolveVariable(timer.getPeriod())), Integer.MAX_VALUE);
                    }
                } else {
                    String resolvedDelay = resolveVariable(timer.getDelay());

                    // when using ISO date/time period is not set
                    long[] repeatValues = null;
                    try {
                        repeatValues = DateTimeUtils.parseRepeatableDateTime(timer.getDelay());
                    } catch (RuntimeException e) {
                        // cannot parse delay, trying to interpret it
                        repeatValues = DateTimeUtils.parseRepeatableDateTime(resolvedDelay);
                    }
                    if (repeatValues.length == 3) {
                        int parsedReapedCount = (int) repeatValues[0];
                        if (parsedReapedCount <= -1) {
                            parsedReapedCount = Integer.MAX_VALUE;
                        }

                        return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2], parsedReapedCount);
                    } else if (repeatValues.length == 2) {
                        return DurationExpirationTime.repeat(repeatValues[0], repeatValues[1], Integer.MAX_VALUE);
                    } else {
                        return DurationExpirationTime.repeat(repeatValues[0], repeatValues[0], Integer.MAX_VALUE);
                    }

                }

            case Timer.TIME_DURATION:

                try {
                    duration = DateTimeUtils.parseDuration(timer.getDelay());
                } catch (RuntimeException e) {
                    // cannot parse delay, trying to interpret it
                    s = resolveVariable(timer.getDelay());
                    duration = DateTimeUtils.parseDuration(s);
                }
                return DurationExpirationTime.after(duration);

            case Timer.TIME_DATE:
                try {
                    return ExactExpirationTime.of(timer.getDate());
                } catch (RuntimeException e) {
                    // cannot parse delay, trying to interpret it
                    s = resolveVariable(timer.getDate());
                    return ExactExpirationTime.of(s);
                }
        }
        throw new UnsupportedOperationException("Not supported timer definition");

    }

    protected String resolveVariable(String s) {
        if (s == null) {
            return null;
        }
        ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
        context.setNodeInstance(this);
        context.setProcessInstance(getProcessInstance());
        return ((Node) getNode()).getVariableExpression().evaluate(s, context);

    }

    protected void handleSLAViolation() {
        if (slaCompliance == io.automatik.engine.api.runtime.process.ProcessInstance.SLA_PENDING) {
            InternalProcessRuntime processRuntime = getProcessInstance().getProcessRuntime();
            processRuntime.getProcessEventSupport().fireBeforeSLAViolated(getProcessInstance(), this, processRuntime);
            logger.debug("SLA violated on node instance {}", getId());
            this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.SLA_VIOLATED;
            this.slaTimerId = null;
            processRuntime.getProcessEventSupport().fireAfterSLAViolated(getProcessInstance(), this, processRuntime);
        }
    }

    @Override
    public void signalEvent(String type, Object event) {
        if ("timerTriggered".equals(type)) {
            TimerInstance timerInstance = (TimerInstance) event;
            if (timerInstances != null && timerInstances.contains(timerInstance.getId())) {
                triggerTimer(timerInstance);
            } else if (timerInstance.getId().equals(slaTimerId)) {
                handleSLAViolation();
            }
        } else if (("slaViolation:" + getId()).equals(type)) {

            handleSLAViolation();
        } else if (("retry:" + getId()).equals(type)) {

            retry();
        } else {
            List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
            if (exitEvents != null && exitEvents.contains(type)) {
                boolean hasCondition = exitOnCompletionCondition();
                if (!hasCondition) {
                    cancel();
                }
            }
        }
    }

    public boolean exitOnCompletionCondition() {
        if (((Node) getNode()).getCompletionCheck().isPresent()) {

            if (((Node) getNode()).getCompletionCheck().get().isValid(getProcessInstance().getVariables())) {
                cancel();
            }

            return true;
        }

        return false;
    }

    private void triggerTimer(TimerInstance timerInstance) {
        for (Map.Entry<Timer, ProcessAction> entry : getEventBasedNode().getTimers().entrySet()) {
            if (entry.getKey().getId() == timerInstance.getTimerId()) {
                if (timerInstance.getRepeatLimit() == 0) {
                    timerInstances.remove(timerInstance.getId());
                }
                executeAction((Action) entry.getValue().getMetaData("Action"));
                return;
            }
        }
    }

    @Override
    public String[] getEventTypes() {
        List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
        if (exitEvents != null && !exitEvents.isEmpty()) {
            List<String> copy = new ArrayList<String>(exitEvents);
            copy.add("timerTriggered");

            return copy.toArray(new String[copy.size()]);
        }

        return new String[] { "timerTriggered" };
    }

    public void triggerCompleted() {
        triggerCompleted(CONNECTION_DEFAULT_TYPE, true);
    }

    @Override
    public void addEventListeners() {
        if (timerInstances != null && (!timerInstances.isEmpty())
                || (this.slaTimerId != null && !this.slaTimerId.trim().isEmpty())) {
            addTimerListener();
        }
        if (slaCompliance == io.automatik.engine.api.runtime.process.ProcessInstance.SLA_PENDING) {
            getProcessInstance().addEventListener("slaViolation:" + getId(), this, true);
        }
        if (retryJobId != null && !retryJobId.isEmpty()) {

            getProcessInstance().addEventListener("retry:" + getId(), this, true);
        }
        if (getExtendedNode().hasCondition()) {

            getProcessInstance().getProcessRuntime().addEventListener(ContextAwareEventListener.using(getId(), listener -> {
                ProcessContext context = new ProcessContext(getProcessInstance().getProcessRuntime());
                context.setProcessInstance(getProcessInstance());
                if (getExtendedNode().isMet(context)) {
                    getProcessInstance().getProcessRuntime().removeEventListener(listener);
                    getProcessInstance().signalEvent((String) getExtendedNode().getMetaData("ConditionEventType"), null);

                }
            }));
        }
        addCompletionListeners();
    }

    protected void addTimerListener() {
        getProcessInstance().addEventListener("timerTriggered", this, false);
        getProcessInstance().addEventListener("timer", this, true);
        getProcessInstance().addEventListener("slaViolation:" + getId(), this, true);
    }

    @Override
    public void removeEventListeners() {
        getProcessInstance().removeEventListener("timerTriggered", this, false);
        getProcessInstance().removeEventListener("timer", this, true);
        getProcessInstance().removeEventListener("slaViolation:" + getId(), this, true);
        if (getProcessInstance().getProcessRuntime() != null) {
            getProcessInstance().getProcessRuntime().removeEventListener(ContextAwareEventListener.using(getId(), null));
        }
        removeCompletionListeners();
    }

    @Override
    public void triggerCompleted(String type, boolean remove) {
        if (this.slaCompliance == io.automatik.engine.api.runtime.process.ProcessInstance.SLA_PENDING) {
            if (System.currentTimeMillis() > slaDueDate.getTime()) {
                // completion of the node instance is after expected SLA due date, mark it
                // accordingly
                this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.SLA_VIOLATED;
            } else {
                this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
            }
        }
        cancelSlaTimer();
        ((io.automatik.engine.workflow.process.instance.NodeInstanceContainer) getNodeInstanceContainer())
                .setCurrentLevel(getLevel());
        cancelTimers();
        removeCompletionListeners();
        getProcessInstance().removeEventListener("slaViolation:" + getId(), this, true);
        super.triggerCompleted(type, remove);
    }

    public List<String> getTimerInstances() {
        return timerInstances;
    }

    public void internalSetTimerInstances(List<String> timerInstances) {
        this.timerInstances = timerInstances;
    }

    @Override
    public void cancel() {
        if (this.slaCompliance == io.automatik.engine.api.runtime.process.ProcessInstance.SLA_PENDING) {
            if (System.currentTimeMillis() > slaDueDate.getTime()) {
                // completion of the process instance is after expected SLA due date, mark it
                // accordingly
                this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.SLA_VIOLATED;
            } else {
                this.slaCompliance = io.automatik.engine.api.runtime.process.ProcessInstance.SLA_ABORTED;
            }
        }
        cancelSlaTimer();
        cancelTimers();
        removeEventListeners();
        removeCompletionListeners();
        super.cancel();
    }

    private void cancelTimers() {
        // deactivate still active timers
        if (timerInstances != null) {
            JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
            for (String id : timerInstances) {
                jobService.cancelJob(id);
            }
        }

        if (retryJobId != null) {
            JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
            jobService.cancelJob(retryJobId);
        }
    }

    private void cancelSlaTimer() {
        if (this.slaTimerId != null && !this.slaTimerId.trim().isEmpty()) {
            JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
            jobService.cancelJob(this.slaTimerId);
            logger.debug("SLA Timer {} has been canceled", this.slaTimerId);
        }
    }

    protected void mapDynamicOutputData(Map<String, Object> results) {
        if (results != null && !results.isEmpty()) {
            VariableScope variableScope = (VariableScope) ((ContextContainer) getProcessInstance().getProcess())
                    .getDefaultContext(VariableScope.VARIABLE_SCOPE);
            VariableScopeInstance variableScopeInstance = (VariableScopeInstance) getProcessInstance()
                    .getContextInstance(VariableScope.VARIABLE_SCOPE);
            for (Entry<String, Object> result : results.entrySet()) {
                String variableName = result.getKey();
                Variable variable = variableScope.findVariable(variableName);
                if (variable != null) {
                    variableScopeInstance.getVariableScope().validateVariable(getProcessInstance().getProcessName(),
                            variableName, result.getValue());
                    variableScopeInstance.setVariable(this, variableName, result.getValue());
                }
            }
        }
    }

    public Map<String, String> extractTimerEventInformation() {
        if (getTimerInstances() != null) {
            for (String id : getTimerInstances()) {
                String[] ids = id.split("_");

                for (Timer entry : getEventBasedNode().getTimers().keySet()) {
                    if (entry.getId() == Long.valueOf(ids[1])) {
                        Map<String, String> properties = new HashMap<>();
                        properties.put("TimerID", id);
                        properties.put("Delay", entry.getDelay());
                        properties.put("Period", entry.getPeriod());
                        properties.put("Date", entry.getDate());

                        return properties;
                    }

                }
            }
        }

        return null;
    }

    private void addCompletionListeners() {
        if (getNode() != null) {
            List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
            if (exitEvents != null) {

                for (String event : exitEvents) {
                    getProcessInstance().addEventListener(event, this, false);
                }
            }
        }
    }

    private void removeCompletionListeners() {
        if (getNode() != null) {
            List<String> exitEvents = (List<String>) getNode().getMetaData().get(NodeImpl.EXIT_EVENTS);
            if (exitEvents != null) {

                for (String event : exitEvents) {
                    getProcessInstance().removeEventListener(event, this, false);
                }
            }
        }
    }

    @Override
    public void registerRetryEventListener() {
        if (retryJobId != null && !retryJobId.isEmpty()) {
            getProcessInstance().addEventListener("retry:" + getId(), this, true);
        }
    }

    @Override
    public void internalSetRetryJobId(String retryJobId) {
        if (retryJobId != null && !retryJobId.isEmpty()) {
            this.retryJobId = retryJobId;
            if (!getNodeInstanceState().equals(NodeInstanceState.Retrying)) {
                internalChangeState(NodeInstanceState.Retrying);
            }
        }
    }

}
