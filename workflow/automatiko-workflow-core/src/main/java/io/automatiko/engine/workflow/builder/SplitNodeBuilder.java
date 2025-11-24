package io.automatiko.engine.workflow.builder;

import java.util.function.Supplier;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.instance.impl.ReturnValueConstraintEvaluator;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.Split;

public class SplitNodeBuilder extends AbstractNodeBuilder {

    private Split node;

    private ReturnValueConstraintEvaluator returnValueConstraint;

    public SplitNodeBuilder(String name, int type, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new Split();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setType(type);

        workflowBuilder.container().addNode(node);

        connect();
    }

    public WorkflowBuilder when(Supplier<Boolean> expression) {

        return when(BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()));

    }

    public WorkflowBuilder when(String expression) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(this);

        this.returnValueConstraint = new ReturnValueConstraintEvaluator();
        returnValueConstraint.setDialect("java");
        returnValueConstraint.setName("");
        returnValueConstraint.setPriority(1);
        returnValueConstraint.setDefault(false);
        returnValueConstraint.setConstraint(expression);

        return workflowBuilder;
    }

    public WorkflowBuilder otherwise() {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(this);

        this.returnValueConstraint = new ReturnValueConstraintEvaluator();
        returnValueConstraint.setDialect("java");
        returnValueConstraint.setName("");
        returnValueConstraint.setPriority(1);
        returnValueConstraint.setDefault(true);
        returnValueConstraint.setConstraint("true");

        return workflowBuilder;
    }

    @Override
    protected void apply(Connection connection) {
        node.setConstraint(connection, returnValueConstraint);
        super.apply(connection);
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
    public SplitNodeBuilder customAttribute(String name, Object value) {
        return (SplitNodeBuilder) super.customAttribute(name, value);
    }
}
