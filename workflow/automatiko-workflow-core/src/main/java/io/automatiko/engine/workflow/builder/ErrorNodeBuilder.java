package io.automatiko.engine.workflow.builder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
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

        workflowBuilder.container().addNode(node);

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

    /**
     * Defines retry to be performed after given duration expressed in ISO 8601 format (<code>PT10S</code> to retry after 10
     * seconds)
     * It will perform at most 3 retries
     * 
     * @param afterExpression ISO 8601 duration expression
     * @return the builder
     */
    public ErrorNodeBuilder retry(String afterExpression) {
        return retry(afterExpression, 3);
    }

    /**
     * Defines retry to be performed after given duration expressed as amount in given time unit
     * It will perform at most 3 retries
     * 
     * @param after amount of time to wait before next retry
     * @param unit time unit of the amount of time to wait
     * @return the builder
     */
    public ErrorNodeBuilder retry(long after, TimeUnit unit) {
        return retry(after, unit, 3);
    }

    /**
     * Defines retry to be performed after given duration expressed in ISO 8601 format (<code>PT10S</code> to retry after 10
     * seconds)
     * It will perform at most retries as given in the argument
     * 
     * @param afterExpression ISO 8601 duration expression
     * @param atMost maximum number of retries
     * @return the builder
     */
    public ErrorNodeBuilder retry(String afterExpression, int atMost) {
        this.node.setMetaData("ErrorRetry", DateTimeUtils.parseDuration(afterExpression));
        this.node.setMetaData("ErrorRetryLimit", atMost);

        return this;
    }

    /**
     * Defines retry to be performed after given duration expressed as amount in given time unit
     * It will perform at most 3 retries
     * 
     * @param after amount of time to wait before next retry
     * @param unit time unit of the amount of time to wait
     * @param atMost maximum number of retries
     * @return the builder
     */
    public ErrorNodeBuilder retry(long after, TimeUnit unit, int atMost) {
        this.node.setMetaData("ErrorRetry", TimeUnit.MILLISECONDS.convert(after, unit));
        this.node.setMetaData("ErrorRetryLimit", atMost);

        return this;
    }

    /**
     * Defines retry to be performed after given duration expressed in ISO 8601 format (<code>PT10S</code> to retry after 10
     * seconds)
     * It will perform at most retries as given in the argument, adding given increment between each retry
     * 
     * @param afterExpression ISO 8601 duration expression
     * @param atMost maximum number of retries
     * @param increamentExpression ISO 8601 duration to be used between retries
     * @return the builder
     */
    public ErrorNodeBuilder retry(String afterExpression, int atMost, String increamentExpression) {
        this.node.setMetaData("ErrorRetry", DateTimeUtils.parseDuration(afterExpression));
        this.node.setMetaData("ErrorRetryLimit", atMost);
        this.node.setMetaData("ErrorRetryIncrement", DateTimeUtils.parseDuration(increamentExpression));

        return this;
    }

    /**
     * Defines retry to be performed after given duration expressed as amount in given time unit
     * It will perform at most 3 retries
     * 
     * @param after amount of time to wait before next retry
     * @param atMost maximum number of retries
     * @param increament amount of time to be used between retries
     * @param unit time unit of the amount of time to wait
     * @return the builder
     */
    public ErrorNodeBuilder retry(long after, int atMost, long increment, TimeUnit unit) {
        this.node.setMetaData("ErrorRetry", TimeUnit.MILLISECONDS.convert(after, unit));
        this.node.setMetaData("ErrorRetryLimit", atMost);
        this.node.setMetaData("ErrorRetryIncrement", TimeUnit.MILLISECONDS.convert(increment, unit));

        return this;
    }

    /**
     * Defines retry to be performed after given duration expressed in ISO 8601 format (<code>PT10S</code> to retry after 10
     * seconds)
     * It will perform at most retries as given in the argument each retry is done after duration based on calculated duration
     * - afterExpression multiplied by the given multiplier
     * 
     * @param afterExpression ISO 8601 duration expression
     * @param atMost maximum number of retries
     * @param multiplier value by which the delay is multiplied before each attempt
     * @return the builder
     */
    public ErrorNodeBuilder retry(String afterExpression, int atMost, double multiplier) {
        this.node.setMetaData("ErrorRetry", DateTimeUtils.parseDuration(afterExpression));
        this.node.setMetaData("ErrorRetryLimit", atMost);
        this.node.setMetaData("ErrorRetryIncrementMultiplier", multiplier);

        return this;
    }

    /**
     * Defines retry to be performed after given duration expressed as amount in given time unit
     * It will perform at most 3 retries
     * 
     * @param after amount of time to wait before next retry
     * @param unit time unit of the amount of time to wait
     * @param atMost maximum number of retries
     * @param multiplier value by which the delay is multiplied before each attempt
     * @return the builder
     */
    public ErrorNodeBuilder retry(long after, TimeUnit unit, int atMost, double multiplier) {
        this.node.setMetaData("ErrorRetry", TimeUnit.MILLISECONDS.convert(after, unit));
        this.node.setMetaData("ErrorRetryLimit", atMost);
        this.node.setMetaData("ErrorRetryIncrementMultiplier", multiplier);

        return this;
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
    public ErrorNodeBuilder customAttribute(String name, Object value) {
        return (ErrorNodeBuilder) super.customAttribute(name, value);
    }
}
