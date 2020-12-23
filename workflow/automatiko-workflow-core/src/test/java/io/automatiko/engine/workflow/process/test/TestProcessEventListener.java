
package io.automatiko.engine.workflow.process.test;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.event.process.ProcessStartedEvent;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;

public class TestProcessEventListener implements ProcessEventListener {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private List<String> eventHistory = new ArrayList<String>();

	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		logAndAdd("bps");
	}

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		logAndAdd("aps");
	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		logAndAdd("bpc");
	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		logAndAdd("apc");
	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		logAndAdd("bnt-" + ((NodeInstanceImpl) event.getNodeInstance()).getUniqueId());
	}

	@Override
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		logAndAdd("ant-" + ((NodeInstanceImpl) event.getNodeInstance()).getUniqueId());
	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		logAndAdd("bnl-" + ((NodeInstanceImpl) event.getNodeInstance()).getUniqueId());
	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		logAndAdd("anl-" + ((NodeInstanceImpl) event.getNodeInstance()).getUniqueId());
	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		logAndAdd("bvc-" + event.getVariableId());
	}

	@Override
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		logAndAdd("avc-" + event.getVariableId());
	}

	public List<String> getEventHistory() {
		return eventHistory;
	}

	private void logAndAdd(String event) {
		logger.trace(event);
		eventHistory.add(event);
	}
}
