package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.core.node.EndNode;

/**
 * Builder responsible for building an end and terminate node
 */
public class EndNodeBuilder extends AbstractNodeBuilder {

    private ExtendedNodeImpl node;

    public EndNodeBuilder(String name, boolean terminate, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new EndNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        ((EndNode) this.node).setTerminate(terminate);

        workflowBuilder.container().addNode(node);

        connect();
    }

    /**
     * Completes given workflow path and returns the builder
     * 
     * @return the builder
     */
    public WorkflowBuilder done() {
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
    public EndNodeBuilder customAttribute(String name, Object value) {
        return (EndNodeBuilder) super.customAttribute(name, value);
    }
}
