package io.automatiko.engine.workflow.process.core.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.ProcessAction;

public class EventSubProcessNode extends CompositeContextNode {

	private static final long serialVersionUID = 2200928773922042238L;

	private List<String> events = new ArrayList<String>();
	private List<EventTypeFilter> eventTypeFilters = new ArrayList<EventTypeFilter>();
	private boolean keepActive = true;

	public void addEvent(EventTypeFilter filter) {
		String type = filter.getType();
		this.events.add(type);
		this.eventTypeFilters.add(filter);
	}

	public List<String> getEvents() {
		return events;
	}

	public boolean isKeepActive() {
		return keepActive;
	}

	public void setKeepActive(boolean triggerOnActivation) {
		this.keepActive = triggerOnActivation;
	}

	public StartNode findStartNode() {
		for (Node node : getNodes()) {
			if (node instanceof StartNode) {
				StartNode startNode = (StartNode) node;
				return startNode;
			}
		}
		return null;
	}

	@Override
	public void addTimer(Timer timer, ProcessAction action) {
		super.addTimer(timer, action);
		if (timer.getTimeType() == Timer.TIME_CYCLE) {
			setKeepActive(false);
		}
	}

	@Override
	public boolean acceptsEvent(String type, Object event) {
		for (EventTypeFilter filter : this.eventTypeFilters) {
			if (filter.acceptsEvent(type, event)) {
				return true;
			}
		}
		return super.acceptsEvent(type, event);
	}

	@Override
	public boolean acceptsEvent(String type, Object event, Function<String, String> resolver) {
		if (resolver == null) {
			return acceptsEvent(type, event);
		}

		for (EventTypeFilter filter : this.eventTypeFilters) {
			if (filter.acceptsEvent(type, event, resolver)) {
				return true;
			}
		}
		return super.acceptsEvent(type, event);
	}

	@Override
	public String getVariableName() {
		StartNode startNode = findStartNode();
		if (startNode != null) {
			return (String) startNode.getMetaData("TriggerMapping");
		}

		return super.getVariableName();
	}

}
