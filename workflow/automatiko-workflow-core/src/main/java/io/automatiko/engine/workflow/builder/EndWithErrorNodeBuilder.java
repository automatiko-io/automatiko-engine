package io.automatiko.engine.workflow.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an end and terminate node
 */
public class EndWithErrorNodeBuilder extends AbstractNodeBuilder {

    private FaultNode node;

    public EndWithErrorNodeBuilder(String name, boolean terminate, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new FaultNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));
        this.node.setTerminateParent(true);

        this.node.setMetaData("functionFlowContinue", "true");

        this.node.setMetaData(Metadata.DATA_INPUTS, new HashMap<>());

        workflowBuilder.container().addNode(node);

        contect();
    }

    public EndWithErrorNodeBuilder errorCode(String errorCode) {

        this.node.setErrorName(this.node.getName());
        this.node.setFaultName(errorCode);
        return this;
    }

    public EndWithErrorNodeBuilder error(String errorName, String errorCode) {

        this.node.setErrorName(errorName);
        this.node.setFaultName(errorCode);
        return this;
    }

    public EndWithErrorNodeBuilder fromDataObject(String name) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(name);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }
        addToDataInputs(name, var.getType().getStringType());
        this.node.setFaultVariable(name);

        return this;
    }

    public <T> EndWithErrorNodeBuilder expressionAsInput(Class<T> type, Supplier<T> expression) {

        this.node.setFaultVariable(
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");

        return this;
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

    @SuppressWarnings("unchecked")
    private void addToDataInputs(String name, String type) {
        ((Map<String, String>) this.node.getMetaData(Metadata.DATA_INPUTS)).put(name, type);
    }
}
