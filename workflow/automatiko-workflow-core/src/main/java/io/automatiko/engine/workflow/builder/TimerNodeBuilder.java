package io.automatiko.engine.workflow.builder;

import java.util.concurrent.TimeUnit;

import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.TimerNode;

/**
 * Builder responsible for building a timer node
 */
public class TimerNodeBuilder extends AbstractNodeBuilder {

    private TimerNode node;
    private Timer timer;

    public TimerNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.timer = new Timer();

        this.node = new TimerNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setTimer(timer);
        workflowBuilder.get().addNode(node);

        contect();
    }

    /**
     * Delays execution for the given amount of time expressed in the time unit
     * 
     * @param amount amount of time to wait before moving on
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public TimerNodeBuilder after(long amount, TimeUnit unit) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit)));
        return this;
    }

    /**
     * Delays execution for the given amount of time expressed in ISO 8601 format e.g. <code>PT10M</code> which stands for 5
     * minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public TimerNodeBuilder after(String isoExpression) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(isoExpression);
        return this;
    }

    /**
     * Repeats execution based on given amount of time expressed in time unit
     * 
     * @param amount amount of time to wait before moving on
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public TimerNodeBuilder every(long amount, TimeUnit unit) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        String expession = String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit));
        this.timer.setDelay(expession);
        this.timer.setPeriod(expession);
        return this;
    }

    /**
     * Repeats execution based on given amount of time expressed in ISO 8601 format e.g. <code>R/PT10M</code> which stands for
     * every 5 minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public TimerNodeBuilder every(String isoExpression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(isoExpression);
        return this;
    }

    /**
     * Repeats execution based on given amount of time expressed in CRON format e.g. <code>0 23 * ? * MON-FRI *</code> which
     * stands for
     * every hour at minute 23 every day between Monday and Friday
     * 
     * @param cronExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public TimerNodeBuilder cron(String cronExpression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(cronExpression);
        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }
}
