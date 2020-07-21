
package io.automatik.engine.workflow.process.executable.core.factory;

import static io.automatik.engine.workflow.process.executable.core.Metadata.ATTACHED_TO;

import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.NodeContainer;
import io.automatik.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatik.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class BoundaryEventNodeFactory extends EventNodeFactory {

	public static final String METHOD_ATTACHED_TO = "attachedTo";

	private NodeContainer nodeContainer;

	private String attachedToUniqueId;

	public BoundaryEventNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer,
			long id) {
		super(nodeContainerFactory, nodeContainer, id);
		this.nodeContainer = nodeContainer;
	}

	protected BoundaryEventNode getBoundaryEventNode() {
		return (BoundaryEventNode) getNode();
	}

	@Override
	protected Node createNode() {
		return new BoundaryEventNode();
	}

	@Override
	public BoundaryEventNodeFactory name(String name) {
		super.name(name);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory variableName(String variableName) {
		super.variableName(variableName);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory eventFilter(EventFilter eventFilter) {
		super.eventFilter(eventFilter);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory eventTransformer(EventTransformer transformer) {
		super.eventTransformer(transformer);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory scope(String scope) {
		super.scope(scope);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory metaData(String name, Object value) {
		super.metaData(name, value);
		return this;
	}

	public BoundaryEventNodeFactory attachedTo(long attachedToId) {
		return attachedTo((String) nodeContainer.getNode(attachedToId).getMetaData().get("UniqueId"));
	}

	public BoundaryEventNodeFactory attachedTo(String attachedToId) {
		attachedToUniqueId = attachedToId;
		getBoundaryEventNode().setAttachedToNodeId(attachedToUniqueId);
		getBoundaryEventNode().setMetaData(ATTACHED_TO, attachedToUniqueId);
		return this;
	}

	@Override
	public BoundaryEventNodeFactory eventType(String eventType) {
		super.eventType(eventType);
		return this;
	}

	public BoundaryEventNodeFactory eventType(String eventTypePrefix, String eventTypeSurffix) {
		if (attachedToUniqueId == null) {
			throw new IllegalStateException("attachedTo() must be called before");
		}
		EventTypeFilter filter = new EventTypeFilter();
		filter.setType(eventTypePrefix + "-" + attachedToUniqueId + "-" + eventTypeSurffix);
		super.eventFilter(filter);
		return this;
	}
}
