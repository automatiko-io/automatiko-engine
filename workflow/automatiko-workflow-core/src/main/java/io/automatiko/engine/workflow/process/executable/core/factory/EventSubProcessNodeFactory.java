
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class EventSubProcessNodeFactory extends CompositeContextNodeFactory {

	public static final String METHOD_KEEP_ACTIVE = "keepActive";
	public static final String METHOD_EVENT = "event";

	public EventSubProcessNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
	}

	@Override
	protected CompositeContextNode createNode() {
		return new EventSubProcessNode();
	}

	public EventSubProcessNodeFactory keepActive(boolean keepActive) {
		((EventSubProcessNode) getCompositeNode()).setKeepActive(keepActive);
		return this;
	}

	public EventSubProcessNodeFactory event(String event) {
		EventTypeFilter filter = new EventTypeFilter();
		filter.setType(event);
		((EventSubProcessNode) getCompositeNode()).addEvent(filter);
		return this;
	}
}
