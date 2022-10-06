package io.automatiko.engine.workflow.builder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.Split;

public abstract class AbstractNodeBuilder {

    protected final AtomicLong ids;
    protected final WorkflowBuilder workflowBuilder;

    public AbstractNodeBuilder(WorkflowBuilder workflowBuilder) {
        this.workflowBuilder = workflowBuilder;
        this.ids = workflowBuilder.ids;
    }

    protected String generateUiqueId(Node node) {
        return UUID.nameUUIDFromBytes((node.getClass().getName() + "_" + node.getId()).getBytes()).toString();
    }

    protected abstract Node getNode();

    protected void apply(Connection connection) {

    }

    protected void diagramItem(Node source, Node node) {

        workflowBuilder.appendDiagramItem((String) source.getMetaData().get("UniqueId"),
                (String) node.getMetaData().get("UniqueId"));
    }

    protected void contect() {

        Node source = this.workflowBuilder.fetchFromContext();
        if (source != null) {
            if (!(getNode() instanceof ForEachNode)) {
                diagramItem(source, getNode());
            }
            ConnectionImpl connection = new ConnectionImpl(source,
                    io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE, getNode(),
                    io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);
            connection.setMetaData("UniqueId", "");

            AbstractNodeBuilder fromContext = this.workflowBuilder.builderFromContext();
            if (fromContext != null) {
                fromContext.apply(connection);
            }
        }
    }

    /**
     * Completes building current node and allows to add new node
     * 
     * @return the workflow builder
     */
    public WorkflowBuilder then() {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        return this.workflowBuilder;
    }

    /**
     * Completes building current node and creates a split in the workflow to allow to define different paths
     * where only one path will be taken meaning the conditions must be exclusive
     * 
     * @param name name of the split node
     * @return the builder for split node
     */
    public SplitNodeBuilder thenSplit(String name) {
        workflowBuilder.putOnContext(getNode());
        SplitNodeBuilder splitBuilder = new SplitNodeBuilder(name, Split.TYPE_XOR, workflowBuilder);

        return splitBuilder;
    }

    /**
     * Completes building current node and creates a split in the workflow to allow to define different paths
     * where only paths that match conditions will be taken
     * 
     * @param name name of the split node
     * @return the builder for split node
     */
    public SplitNodeBuilder thenMultipleSplit(String name) {
        workflowBuilder.putOnContext(getNode());
        SplitNodeBuilder splitBuilder = new SplitNodeBuilder(name, Split.TYPE_OR, workflowBuilder);

        return splitBuilder;
    }

    /**
     * Completes building current node and creates a parallel split in the workflow to allow to define different paths
     * where all of the paths will be taken
     * 
     * @param name name of the split node
     * @return the builder for split node
     */
    public ParallelSplitNodeBuilder thenParallelSplit(String name) {
        workflowBuilder.putOnContext(getNode());
        ParallelSplitNodeBuilder splitBuilder = new ParallelSplitNodeBuilder(name, workflowBuilder);

        return splitBuilder;
    }

    /**
     * Completes building current node and creates a split waiting on events in the workflow to allow to define different paths
     * where only one path will be taken based on which event will arrive first
     * 
     * @param name name of the split node
     * @return the builder for split node
     */
    public EventSplitNodeBuilder thenSplitOnEvents(String name) {
        workflowBuilder.putOnContext(getNode());
        EventSplitNodeBuilder splitBuilder = new EventSplitNodeBuilder(name, workflowBuilder);

        return splitBuilder;
    }

    /**
     * Adds joining node to merge single incoming paths.
     * 
     * @param name name of the join node
     * @return the builder
     */
    public JoinNodeBuilder thenJoin(String name) {
        return thenJoin(name, Join.TYPE_XOR);
    }

    /**
     * Adds joining node to merge multiple incoming paths.
     * 
     * @param name name of the join node
     * @return the builder
     */
    public JoinNodeBuilder thenJoinMultiple(String name) {
        return thenJoin(name, Join.TYPE_OR);
    }

    /**
     * Adds joining node to merge all incoming paths.
     * 
     * @param name name of the join node
     * @return the builder
     */
    public JoinNodeBuilder thenJoinAll(String name) {
        return thenJoin(name, Join.TYPE_AND);
    }

    protected AbstractNodeBuilder customAttribute(String name, Object value) {
        if (name != null && value != null) {
            getNode().setMetaData(name, value);
        }
        return this;
    }

    private JoinNodeBuilder thenJoin(String name, int type) {
        workflowBuilder.putOnContext(getNode());
        workflowBuilder.putBuilderOnContext(null);
        JoinNodeBuilder joinBuilder;
        if (workflowBuilder.joins.containsKey(name)) {
            joinBuilder = workflowBuilder.joins.get(name);
            joinBuilder.contect();

        } else {

            joinBuilder = new JoinNodeBuilder(name, type, workflowBuilder);
            workflowBuilder.joins.put(name, joinBuilder);
        }
        return joinBuilder;
    }

    protected Class<?> resolveItemType(String dataObjectName) {
        String itemType = Object.class.getCanonicalName();
        Variable itemVar = workflowBuilder.get().getVariableScope().findVariable(dataObjectName);
        if (itemVar != null) {
            itemType = (String) itemVar.getMetaData().getOrDefault("type", itemType);
        }

        try {
            return Class.forName(itemType, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot resolve type " + itemType);
        }
    }
}
