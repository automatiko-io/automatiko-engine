package io.automatiko.engine.workflow.builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
        workflowBuilder.container().addNode(node);

        connect();
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
     * Delays execution for the given amount of time expressed in ISO 8601 format e.g. <code>PT10M</code> which stands for 10
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
     * Delays execution for the given amount of time expressed in ISO 8601 format that is calculated from the expression e.g.
     * <code>PT10M</code> which stands for 10
     * minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public TimerNodeBuilder afterFromExpression(String expression) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(
                "#{" + expression + "}");
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
    public TimerNodeBuilder afterFromExpression(Supplier<String> expression) {
        this.timer.setTimeType(Timer.TIME_DURATION);
        this.timer.setDelay(
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");
        return this;
    }

    /**
     * Delays execution until the date and time given in ISO 8601 format e.g. <code>2011-12-03T10:15:30+01:00</code>
     * 
     * @param dateTimeInIso ISO 8601 date and time expression
     * @return the builder
     */
    public TimerNodeBuilder at(String dateTimeInIso) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate(dateTimeInIso);
        return this;
    }

    /**
     * Delays execution until the date (start of the day)
     * 
     * @param date date of expiration
     * @return the builder
     */
    public TimerNodeBuilder at(LocalDate date) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate(date.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME));
        return this;
    }

    /**
     * Delays execution until the date and time
     * 
     * @param dateTime date and time of expiration
     * @return the builder
     */
    public TimerNodeBuilder at(LocalDateTime dateTime) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        return this;
    }

    /**
     * Delays execution until the date and time
     * 
     * @param dateTime date and time of expiration
     * @return the builder
     */
    public TimerNodeBuilder at(OffsetDateTime dateTime) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        return this;
    }

    /**
     * Delays execution until the date and time that is calculated by given expression
     * 
     * @param date expression to calculate date and time
     * @return the builder
     */
    public TimerNodeBuilder atFromExpression(String expression) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate("#{" + expression + "}");
        return this;
    }

    /**
     * Delays execution until the date and time that is calculated by given expression
     * 
     * @param date expression to calculate date and time
     * @return the builder
     */
    public TimerNodeBuilder atFromExpression(Supplier<? extends Temporal> expression) {
        this.timer.setTimeType(Timer.TIME_DATE);
        this.timer.setDate(
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");
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
     * every 10 minutes
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
     * Repeats execution based on given amount of time expressed in ISO 8601 format that is calculated from the expression
     * e.g. <code>R/PT10M</code> which stands for every 10 minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public TimerNodeBuilder everyFromExpression(String expression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(
                "#{" + expression + "}");
        return this;
    }

    /**
     * Repeats execution based on given amount of time expressed in ISO 8601 format that is calculated from the expression
     * e.g. <code>R/PT10M</code> which stands for every 10 minutes
     * 
     * @param expression expression to calculate time expression in ISO 8601
     * @return the builder
     */
    public TimerNodeBuilder everyFromExpression(Supplier<String> expression) {
        this.timer.setTimeType(Timer.TIME_CYCLE);
        this.timer.setDelay(
                "#{" + BuilderContext.get(Thread.currentThread().getStackTrace()[2].getMethodName()).replace("\"", "\\\"")
                        + "}");
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

    /**
     * Sets custom attribute for this node
     * 
     * @param name name of the attribute, must not be null
     * @param value value of the attribute, must not be null
     * @return the builder
     */
    public TimerNodeBuilder customAttribute(String name, Object value) {
        return (TimerNodeBuilder) super.customAttribute(name, value);
    }
}
