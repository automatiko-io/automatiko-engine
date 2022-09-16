package io.automatiko.engine.workflow.builder;

import java.util.concurrent.TimeUnit;

import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.executable.core.Metadata;

/**
 * Builder responsible for building an start node
 */
public class StartOnTimerNodeBuilder extends AbstractNodeBuilder {

    private StartNode node;
    private Timer timer;

    public StartOnTimerNodeBuilder(String name, WorkflowBuilder workflowBuilder) {
        super(workflowBuilder);
        this.timer = new Timer();
        this.node = new StartNode();

        this.node.setId(ids.incrementAndGet());
        this.node.setName(name);
        this.node.setMetaData("UniqueId", generateUiqueId(this.node));

        this.node.setMetaData(Metadata.TRIGGER_TYPE, "Timer");
        this.node.setTimer(timer);

        workflowBuilder.container().addNode(node);
    }

    /**
     * Starts new instance after the given amount of time expressed in the time unit
     * 
     * @param amount amount of time to wait before starting new instance
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public StartOnTimerNodeBuilder after(long amount, TimeUnit unit) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit)));
        return this;
    }

    /**
     * Starts new instance after the given amount of time expressed in ISO 8601 format e.g. <code>PT10M</code> which stands for
     * 5 minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public StartOnTimerNodeBuilder after(String isoExpression) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(isoExpression);
        return this;
    }

    /**
     * Starts new instance based on given amount of time expressed in time unit
     * 
     * @param amount amount of time to wait before moving on
     * @param unit time unit the amount is given with
     * @return the builder
     */
    public StartOnTimerNodeBuilder every(long amount, TimeUnit unit) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        String expession = String.valueOf(TimeUnit.MILLISECONDS.convert(amount, unit));
        this.timer.setDelay(expession);
        this.timer.setPeriod(expession);
        return this;
    }

    /**
     * Starts new instance based on given amount of time expressed in ISO 8601 format e.g. <code>R/PT10M</code> which stands for
     * every 5 minutes
     * 
     * @param isoExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public StartOnTimerNodeBuilder every(String isoExpression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(isoExpression);
        return this;
    }

    /**
     * Starts new instance based on given amount of time expressed in CRON format e.g. <code>0 23 * ? * MON-FRI *</code> which
     * stands for
     * every hour at minute 23 every day between Monday and Friday
     * 
     * @param cronExpression Expression ISO 8601 time expression
     * @return the builder
     */
    public StartOnTimerNodeBuilder cron(String cronExpression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(cronExpression);
        return this;
    }

    @Override
    protected Node getNode() {
        return this.node;
    }

}
