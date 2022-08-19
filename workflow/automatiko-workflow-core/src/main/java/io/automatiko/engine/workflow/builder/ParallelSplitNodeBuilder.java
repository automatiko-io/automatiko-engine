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

        workflowBuilder.get().addNode(node);

        contect();
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
