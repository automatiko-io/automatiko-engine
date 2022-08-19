package io.automatiko.engine.workflow.builder;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;

/**
 * Builder responsible for building an end and terminate node
 */
public class ErrorNodeBuilder extends AbstractNodeBuilder {

    private BoundaryEventNode node;

    private EventTypeFilter filter;

    public ErrorNodeBuilder(String name, String attachedTo, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.node = new BoundaryEventNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.filter = new EventTypeFilter();

        this.node.addEventFilter(filter);

        this.node.setAttachedToNodeId(attachedTo);

        this.node.setMetaData("EventType", "error");

        this.node.setMetaData("AttachedTo", attachedTo);
        this.node.setMetaData("HasErrorEvent", true);

        workflowBuilder.get().addNode(node);

        Node source = this.workflowBuilder.fetchFromContext();
        if (source != null) {
            diagramItem(source, getNode());
        }
    }

    /**
     * Specifies error codes that this error node should handle
     * 
     * @param errorCodes non null error codes
     * @return the builder
     */
    public ErrorNodeBuilder errorCodes(String... errorCodes) {

        String codes = Stream.of(errorCodes).collect(Collectors.joining(","));

        this.node.setMetaData("ErrorEvent", codes);
        this.filter.setType("Error-" + this.node.getAttachedToNodeId() + "-" + codes);

        return this;
    }

    public ErrorNodeBuilder toDataObject(String name) {
        Variable var = this.workflowBuilder.get().getVariableScope().findVariable(name);

        if (var == null) {
            throw new IllegalArgumentException("Cannot find data object with '" + name + "' name");
        }
        this.node.setVariableName(name);

        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
