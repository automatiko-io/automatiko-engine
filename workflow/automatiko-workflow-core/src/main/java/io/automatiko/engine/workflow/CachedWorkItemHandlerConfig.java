
package io.automatiko.engine.workflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.workflow.WorkItemHandlerConfig;

public class CachedWorkItemHandlerConfig implements WorkItemHandlerConfig {

	private final Map<String, WorkItemHandler> workItemHandlers = new HashMap<>();

	public CachedWorkItemHandlerConfig register(String name, WorkItemHandler handler) {
		workItemHandlers.put(name, handler);
		return this;
	}

	@Override
	public WorkItemHandler forName(String name) {
		WorkItemHandler workItemHandler = workItemHandlers.get(name);
		if (workItemHandler == null) {
			throw new NoSuchElementException(name);
		} else {
			return workItemHandler;
		}
	}

	@Override
	public Collection<String> names() {
		return workItemHandlers.keySet();
	}
}
