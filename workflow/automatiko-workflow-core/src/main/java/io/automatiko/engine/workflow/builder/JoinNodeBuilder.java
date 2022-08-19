package io.automatiko.engine.workflow.builder;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.Join;

public class JoinNodeBuilder extends AbstractNodeBuilder {

    private Join node;

    public JoinNodeBuilder(String name, int type, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new Join();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setType(type);

        workflowBuilder.get().addNode(node);

        contect();
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
