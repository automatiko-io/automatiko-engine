package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;

public class AdditionalPathNodeBuilder extends AbstractNodeBuilder {

    private EventSubProcessNode node;

    public AdditionalPathNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new EventSubProcessNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        workflowBuilder.container().addNode(node);
        workflowBuilder.container(node);
    }

    public AdditionalPathNodeBuilder event(String event) {
        this.node.addEvent(event);
        return this;
    }

    public WorkflowBuilder end() {
        workflowBuilder.container(workflowBuilder.get());
        return workflowBuilder;
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
    public AdditionalPathNodeBuilder customAttribute(String name, Object value) {
        return (AdditionalPathNodeBuilder) super.customAttribute(name, value);
    }
}
