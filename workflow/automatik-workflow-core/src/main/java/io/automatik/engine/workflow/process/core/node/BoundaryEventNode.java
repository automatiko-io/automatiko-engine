package io.automatik.engine.workflow.process.core.node;

import java.util.function.Function;

import io.automatik.engine.workflow.base.core.event.EventFilter;

public class BoundaryEventNode extends EventNode {

	private static final long serialVersionUID = 3448981074702415561L;

	private String attachedToNodeId;

	public String getAttachedToNodeId() {
		return attachedToNodeId;
	}

	public void setAttachedToNodeId(String attachedToNodeId) {
		this.attachedToNodeId = attachedToNodeId;
	}

	@Override
	public boolean acceptsEvent(String type, Object event, Function<String, String> resolver) {
		if (resolver == null) {
			return acceptsEvent(type, event);
		}

		for (EventFilter filter : getEventFilters()) {
			if (filter.acceptsEvent(type, event, resolver)) {
				return true;
			}
		}
		return super.acceptsEvent(type, event);
	}
}
