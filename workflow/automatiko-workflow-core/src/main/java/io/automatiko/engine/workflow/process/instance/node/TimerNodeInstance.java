
package io.automatiko.engine.workflow.process.instance.node;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.NodeInstanceState;
import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.BaseEventDescription;
import io.automatiko.engine.api.workflow.EventDescription;
import io.automatiko.engine.services.time.TimerInstance;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance;

public class TimerNodeInstance extends StateBasedNodeInstance implements EventListener {

    private static final long serialVersionUID = 510l;
    private static final Logger logger = LoggerFactory.getLogger(TimerNodeInstance.class);
    private static final String TIMER_TRIGGERED_EVENT = "timerTriggered";

    private String timerId;

    public TimerNode getTimerNode() {
        return (TimerNode) getNode();
    }

    public String getTimerId() {
        return timerId;
    }

    public void internalSetTimerId(String timerId) {
        this.timerId = timerId;
    }

    @Override
    public void internalTrigger(NodeInstance from, String type) {
        if (!io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException("A TimerNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        ExpirationTime expirationTime = createTimerInstance(getTimerNode().getTimer());
        if (getTimerInstances() == null) {
            addTimerListener();
        }
        JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
        timerId = jobService.scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(
                getTimerNode().getTimer().getId(), expirationTime, getProcessInstanceIdWithParent(),
                getProcessInstance().getRootProcessInstanceId(), getProcessInstance().getProcessId(),
                getProcessInstance().getProcess().getVersion(), getProcessInstance().getRootProcessId()));
        logger.debug("Scheduled timer with id {} for node {} with fire date {}", timerId, getNodeName(), expirationTime.get());
    }

    public void signalEvent(String type, Object event) {
        if (TIMER_TRIGGERED_EVENT.equals(type)) {
            TimerInstance timer = (TimerInstance) event;
            if (timer.getId().equals(timerId)) {
                logger.debug("Triggering timer with id {} on node {}", timerId, getNodeName());
                triggerCompleted(timer.getRepeatLimit() <= 0);
            }
        }
    }

    public String[] getEventTypes() {
        return new String[] { TIMER_TRIGGERED_EVENT };
    }

    public void triggerCompleted(boolean remove) {
        if (remove) {
            removeEventListeners();
            InternalProcessRuntime processRuntime = ((InternalProcessRuntime) getProcessInstance().getProcessRuntime());
            processRuntime.getUnitOfWorkManager().currentUnitOfWork().intercept(WorkUnit.create(timerId, e -> {
                processRuntime.getJobsService().cancelJob(timerId);
            }));

        }

        internalChangeState(NodeInstanceState.Occur);
        triggerCompleted(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, remove);
    }

    @Override
    public void cancel() {
        removeEventListeners();
        getProcessInstance().getProcessRuntime().getJobsService().cancelJob(timerId);
        super.cancel();
    }

    public void addEventListeners() {
        super.addEventListeners();
        if (getTimerInstances() == null) {
            addTimerListener();
        }
    }

    public void removeEventListeners() {
        super.removeEventListeners();
        ((WorkflowProcessInstance) getProcessInstance()).removeEventListener(TIMER_TRIGGERED_EVENT, this, false);
    }

    @Override
    public Set<EventDescription<?>> getEventDescriptions() {
        Map<String, String> properties = new HashMap<>();
        properties.put("TimerID", timerId);
        properties.put("Delay", getTimerNode().getTimer().getDelay());
        properties.put("Period", getTimerNode().getTimer().getPeriod());
        properties.put("Date", getTimerNode().getTimer().getDate());
        properties.put("NextFireTime", ((InternalProcessRuntime) getProcessInstance().getProcessRuntime())
                .getJobsService().getScheduledTime(timerId).toString());

        return Collections.singleton(new BaseEventDescription(TIMER_TRIGGERED_EVENT, getNodeDefinitionId(), getNodeName(),
                "timer", getId(), getProcessInstance().getId(), null, properties));

    }

}
