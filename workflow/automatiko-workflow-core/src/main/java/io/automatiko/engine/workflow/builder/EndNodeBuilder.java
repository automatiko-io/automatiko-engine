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

        contect();
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
}
