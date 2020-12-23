
package io.automatiko.engine.workflow.base.core.event;

/**
 * This variant of the {@link EventTypeFilter} can be used with structures such
 * as Escalations, for which Intermediate (Catching) Events can be triggered by
 * both
 * 
 *
 */
public class BroadcastEventTypeFilter extends EventTypeFilter {

	private static final long serialVersionUID = 510l;

	public boolean acceptsEvent(String type, Object event) {
		if (type == null) {
			return false;
		}
		boolean accepts = false;
		if (this.type != null) {
			if (this.type.equals(type)) {
				accepts = true;
			} else if (type != null && type.startsWith(this.type)) {
				accepts = true;
			}
		}
		return accepts;
	}

	public String toString() {
		return "Broadcast Event filter: [" + this.type + "]";
	}
}
