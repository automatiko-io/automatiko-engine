package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.MongodbJobsConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.jobs.mongodb")
public class MongodbJobsBuildTimeConfig extends MongodbJobsConfig {

    /**
     * Name of the data base to be used to create collections for jobs
     */
    @ConfigItem
    public Optional<String> database;

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
    public Optional<String> database() {
        return database;
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
