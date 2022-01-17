package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.DynamoDBJobsConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.jobs.dynamodb")
public class DynamoDBJobsRuntimeConfig extends DynamoDBJobsConfig {

    /**
     * Indicates if tables should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createTables;

    /**
     * Specifies the read capacity, if not set defaults to 10
     */
    @ConfigItem
    public Optional<Long> readCapacity;

    /**
     * Specifies the write capacity, if not set defaults to 10
     */
    @ConfigItem
    public Optional<Long> writeCapacity;

    /**
     * Interval (in minutes) how often to look for next chunk of jobs to schedule
     */
    @ConfigItem
    public Optional<Long> interval;

    /**
     * Number of threads to be used for jobs execution
     */
    @ConfigItem
    public Optional<Integer> threads;

    @Override
    public Optional<Boolean> createTables() {
        return createTables;
    }

    @Override
    public Optional<Long> readCapacity() {
        return readCapacity;
    }

    @Override
    public Optional<Long> writeCapacity() {
        return writeCapacity;
    }

    @Override
    public Optional<Long> interval() {
        return interval;
    }

    @Override
    public Optional<Integer> threads() {
        return threads;
    }
}
