
package io.automatik.engine.workflow.process.instance.node;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.jobs.ExpirationTime;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.workflow.BaseEventDescription;
import io.automatik.engine.api.workflow.EventDescription;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.workflow.process.core.node.TimerNode;
import io.automatik.engine.workflow.process.instance.WorkflowProcessInstance;

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
		if (!io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
			throw new IllegalArgumentException("A TimerNode only accepts default incoming connections!");
		}
		triggerTime = new Date();
		ExpirationTime expirationTime = createTimerInstance(getTimerNode().getTimer());
		if (getTimerInstances() == null) {
			addTimerListener();
		}
		JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();
		timerId = jobService
				.scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(getTimerNode().getTimer().getId(),
						expirationTime, getProcessInstance().getId(), getProcessInstance().getRootProcessInstanceId(),
						getProcessInstance().getProcessId(), getProcessInstance().getRootProcessId()));
	}

	public void signalEvent(String type, Object event) {
		if (TIMER_TRIGGERED_EVENT.equals(type)) {
			TimerInstance timer = (TimerInstance) event;
			if (timer.getId().equals(timerId)) {
				triggerCompleted(timer.getRepeatLimit() <= 0);
			}
		}
	}

	public String[] getEventTypes() {
		return new String[] { TIMER_TRIGGERED_EVENT };
	}

	public void triggerCompleted(boolean remove) {
		triggerCompleted(io.automatik.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, remove);
	}

	@Override
	public void cancel() {
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
		return Collections.singleton(new BaseEventDescription(TIMER_TRIGGERED_EVENT, getNodeDefinitionId(),
				getNodeName(), "timer", getId(), getProcessInstance().getId(), null, properties));

	}

}
