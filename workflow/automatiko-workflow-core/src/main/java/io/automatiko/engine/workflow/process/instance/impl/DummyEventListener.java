
package io.automatiko.engine.workflow.process.instance.impl;

import io.automatiko.engine.api.runtime.process.EventListener;

public class DummyEventListener implements EventListener {

	public final static DummyEventListener EMPTY_EVENT_LISTENER = new DummyEventListener();

	private DummyEventListener() {
	}

	@Override
	public void signalEvent(String type, Object event) {
	}

	@Override
	public String[] getEventTypes() {
		return null;
	}

}
