package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.Split;

public class ParallelSplitNodeBuilder extends AbstractNodeBuilder {

    private Split node;

    public ParallelSplitNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new Split();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setType(Split.TYPE_AND);

        workflowBuilder.container().addNode(node);

        contect();
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
    public ParallelSplitNodeBuilder customAttribute(String name, Object value) {
        return (ParallelSplitNodeBuilder) super.customAttribute(name, value);
    }
}
