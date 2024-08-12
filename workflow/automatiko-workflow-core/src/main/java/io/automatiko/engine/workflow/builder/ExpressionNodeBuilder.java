package io.automatiko.engine.workflow.builder;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.node.ActionNode;

/**
 * Builder responsible for building an expression node (aka script node)
 */
public class ExpressionNodeBuilder extends AbstractNodeBuilder {

    private ActionNode node;

    public ExpressionNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new ActionNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        workflowBuilder.container().addNode(node);
        connect();
    }

    protected ExpressionNodeBuilder expression(String expression) {
        ConsequenceAction processAction = new ConsequenceAction(null, expression);

        node.setAction(processAction);

        return this;
    }

    protected ExpressionNodeBuilder println(String text) {
        return expression("System.out.println(" + text + ");");

    }

    protected ExpressionNodeBuilder log(String text, String... values) {
        String valuesString = Stream.of(values).collect(Collectors.joining(","));
        if (!valuesString.isEmpty()) {
            valuesString = "," + valuesString;
        }
        return expression("log(\"" + text + "\"" + valuesString + ");");
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
    public ExpressionNodeBuilder customAttribute(String name, Object value) {
        return (ExpressionNodeBuilder) super.customAttribute(name, value);
    }
}
