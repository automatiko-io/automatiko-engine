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

    public WorkflowBuilder end() {
        workflowBuilder.container(workflowBuilder.get());
        return workflowBuilder;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
