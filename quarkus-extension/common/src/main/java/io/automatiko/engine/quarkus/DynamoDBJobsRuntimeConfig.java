package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DynamoDBJobsRuntimeConfig {

    /**
     * Indicates if tables should be created during startup
     */
    Optional<Boolean> createTables();

    /**
     * Specifies the read capacity, if not set defaults to 10
     */
    Optional<Long> readCapacity();

    /**
     * Specifies the write capacity, if not set defaults to 10
     */
    Optional<Long> writeCapacity();

    /**
     * Interval (in minutes) how often to look for next chunk of jobs to schedule
     */
    Optional<Long> interval();

    /**
     * Number of threads to be used for jobs execution
     */
    Optional<Integer> threads();

}
