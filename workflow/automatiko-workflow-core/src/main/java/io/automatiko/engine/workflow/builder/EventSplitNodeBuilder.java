package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.Split;

public class EventSplitNodeBuilder extends AbstractNodeBuilder {

    private Split node;

    public EventSplitNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new Split();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setType(Split.TYPE_XAND);

        workflowBuilder.container().addNode(node);

        connect();
    }

    public TimerNodeBuilder onTimer(String name) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return new TimerNodeBuilder(name, workflowBuilder);
    }

    public WaitOnMessageNodeBuilder onMessage(String name) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return new WaitOnMessageNodeBuilder(name, workflowBuilder);
    }

    @Override
    protected Node getNode() {
        return this.node;
    }

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public EventSplitNodeBuilder customAttribute(String name, Object value) {
        return (EventSplitNodeBuilder) super.customAttribute(name, value);
    }
}
