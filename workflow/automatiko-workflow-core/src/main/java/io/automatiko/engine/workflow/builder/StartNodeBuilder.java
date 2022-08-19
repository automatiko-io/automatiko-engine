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

        workflowBuilder.get().addNode(node);
    }

    @Override
    protected Node getNode() {
        return this.node;
    }

}
