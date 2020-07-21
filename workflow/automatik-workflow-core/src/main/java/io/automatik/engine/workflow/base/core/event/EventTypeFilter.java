
package io.automatik.engine.workflow.base.core.event;

import java.io.Serializable;
import java.util.function.Function;

public class EventTypeFilter implements EventFilter, Serializable {

	private static final long serialVersionUID = 510l;

	protected String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean acceptsEvent(String type, Object event) {
		if (this.type != null && this.type.equals(type)) {
			return true;
		}
		return false;
	}

	public String toString() {
		return "Event filter: [" + this.type + "]";
	}

	@Override
	public boolean acceptsEvent(String type, Object event, Function<String, String> resolver) {
		if (this.type != null && resolver.apply(this.type).equals(type)) {
			return true;
		}
		return false;
	}
}
