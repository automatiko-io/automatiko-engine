package io.automatiko.engine.workflow.base.core.timer;

import java.time.ZonedDateTime;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import io.automatiko.engine.api.jobs.ExpirationTime;

public class CronExpirationTime implements ExpirationTime {

    private final String expression;

    private final Cron cron;

    private final ExecutionTime executionTime;

    public CronExpirationTime(String expression) {
        this.expression = expression;

        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        cron = parser.parse(expression);
        executionTime = ExecutionTime.forCron(cron);

    }

    @Override
    public ZonedDateTime get() {
        return executionTime.nextExecution(ZonedDateTime.now()).get();
    }

    @Override
    public Long repeatInterval() {
        return null;
    }

    @Override
    public Integer repeatLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String expression() {
        return expression;
    }

    @Override
    public ZonedDateTime next() {
        return executionTime.nextExecution(ZonedDateTime.now()).orElse(null);
    }

    public static CronExpirationTime of(String expression) {
        return new CronExpirationTime(expression);
    }

    public static boolean isCronExpression(String expression) {
        try {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
            parser.parse(expression).validate();

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
