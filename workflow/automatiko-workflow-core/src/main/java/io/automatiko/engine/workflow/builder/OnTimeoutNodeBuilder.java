package io.automatiko.engine.workflow.builder;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;

/**
 * Builder responsible for building a timeout attached node
 */
public class OnTimeoutNodeBuilder extends AbstractNodeBuilder {

    private BoundaryEventNode node;

    private EventTypeFilter filter;

    private String attachedTo;

    public OnTimeoutNodeBuilder(String name, String attachedTo, WorkflowBuilder workflowBuilder, boolean cancelActivity) {
        super(workflowBuilder);
        this.node = new BoundaryEventNode();
        this.attachedTo = attachedTo;

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setAttachedToNodeId(attachedTo);

        this.node.setMetaData("AttachedTo", attachedTo);
        this.node.setMetaData("CancelActivity", cancelActivity);

        this.workflowBuilder.container().addNode(node);

        Node source = this.workflowBuilder.fetchFromContext();
        if (source != null) {
            diagramItem(source, getNode());
        }
    }

    /**
     * Delays execution for the given amount of time expressed in the time unit
     * 
     * @param amount amount of time to wait before moving on
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public OnTimeoutNodeBuilder after(long amount, TimeUnit unit) {

        if (filter != null) {
            throw new IllegalStateException("Timer is already defined");
        }
        String timeDuration = String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit));
        this.filter = new EventTypeFilter();

        this.node.addEventFilter(filter);
        filter.setType("Timer-" + attachedTo + "-" + timeDuration + "-" + this.node.getId());

        this.node.setMetaData("TimeDuration", timeDuration);

        return this;
    }

    /**
     * Delays execution for the given amount of time expressed in ISO 8601 format e.g. <code>PT10M</code> which stands for 5
     * minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public OnTimeoutNodeBuilder after(String isoExpression) {
        return after(DateTimeUtils.parseDuration(isoExpression), TimeUnit.MILLISECONDS);
    }

    /**
     * Delays execution for the given amount of time expressed in ISO 8601 format that is calculated from the expression e.g.
     * <code>PT10M</code> which stands for 10
     * minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public OnTimeoutNodeBuilder afterFromExpression(String expression) {
        if (filter != null) {
            throw new IllegalStateException("Timer is already defined");
        }
        String timeDuration = "#{" + expression + "}";
        this.filter = new EventTypeFilter();

        this.node.addEventFilter(filter);
        filter.setType("Timer-" + attachedTo + "-" + timeDuration + "-" + this.node.getId());

        this.node.setMetaData("TimeDuration", timeDuration);
        return this;
    }

    /**
     * Delays execution for the given amount of time expressed in ISO 8601 format that is calculated from the expression e.g.
     * <code>PT10M</code> which stands for 10
     * minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public OnTimeoutNodeBuilder afterFromExpression(Supplier<String> expression) {
        return afterFromExpression(
                BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\""));

    }

    /**
     * Repeats execution based on given amount of time expressed in time unit
     * 
     * @param amount amount of time to wait before moving on
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public OnTimeoutNodeBuilder every(long amount, TimeUnit unit) {

        return every(String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit)));
    }

    /**
     * Repeats execution based on given amount of time expressed in ISO 8601 format e.g. <code>R/PT10M</code> which stands for
     * every 10 minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public OnTimeoutNodeBuilder every(String isoExpression) {
        if (filter != null) {
            throw new IllegalStateException("Timer is already defined");
        }
        String timeCycle = isoExpression;
        this.filter = new EventTypeFilter();

        this.node.addEventFilter(filter);
        filter.setType("Timer-" + attachedTo + "-" + timeCycle + "-" + this.node.getId());

        this.node.setMetaData("TimeCycle", timeCycle);

        return this;
    }

    /**
     * Repeats execution based on given amount of time expressed in ISO 8601 format that is calculated from the expression
     * e.g. <code>R/PT10M</code> which stands for every 10 minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public OnTimeoutNodeBuilder everyFromExpression(String expression) {
        return every("#{" + expression + "}");
    }

    /**
     * Repeats execution based on given amount of time expressed in ISO 8601 format that is calculated from the expression
     * e.g. <code>R/PT10M</code> which stands for every 10 minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public OnTimeoutNodeBuilder everyFromExpression(Supplier<String> expression) {
        return every(
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");

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
    public OnTimeoutNodeBuilder customAttribute(String name, Object value) {
        return (OnTimeoutNodeBuilder) super.customAttribute(name, value);
    }
}
