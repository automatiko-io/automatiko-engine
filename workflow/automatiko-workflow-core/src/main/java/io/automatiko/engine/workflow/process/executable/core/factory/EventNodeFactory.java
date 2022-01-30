
package io.automatiko.engine.workflow.process.executable.core.factory;

import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTransformer;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory;

public class EventNodeFactory extends ExtendedNodeFactory implements MappableNodeFactory {

    public static final String METHOD_EVENT_TYPE = "eventType";
    public static final String METHOD_SCOPE = "scope";
    public static final String METHOD_VARIABLE_NAME = "variableName";

    public EventNodeFactory(ExecutableNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
        super(nodeContainerFactory, nodeContainer, id);
    }

    protected Node createNode() {
        return new EventNode();
    }

    protected EventNode getEventNode() {
        return (EventNode) getNode();
    }

    @Override
    public EventNodeFactory name(String name) {
        super.name(name);
        return this;
    }

    public EventNodeFactory variableName(String variableName) {
        getEventNode().setVariableName(variableName);
        return this;
    }

    public EventNodeFactory eventFilter(EventFilter eventFilter) {
        getEventNode().addEventFilter(eventFilter);
        return this;
    }

    public EventNodeFactory eventType(String eventType) {
        EventTypeFilter filter = new EventTypeFilter();
        filter.setType(eventType);
        return eventFilter(filter);
    }

    public EventNodeFactory eventTransformer(EventTransformer transformer) {
        getEventNode().setEventTransformer(transformer);
        return this;
    }

    public EventNodeFactory scope(String scope) {
        getEventNode().setScope(scope);
        return this;
    }

    @Override
    public Mappable getMappableNode() {
        return getEventNode();
    }
}
