package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.StartNode;

/**
 * Builder responsible for building an start node
 */
public class StartNodeBuilder extends AbstractNodeBuilder {

    private StartNode node;

    public StartNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new StartNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        workflowBuilder.container().addNode(node);
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
    public StartNodeBuilder customAttribute(String name, Object value) {
        return (StartNodeBuilder) super.customAttribute(name, value);
    }

}
