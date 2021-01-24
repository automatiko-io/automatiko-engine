package io.automatiko.engine.api.config;

import java.util.Optional;

public class DynamoDBJobsConfig {

    /**
     * Indicates if tables should be created during startup.
     */
    public Optional<Boolean> createTables() {
        return Optional.empty();
    }

    /**
     * Specifies the read capacity
     */
    public Optional<Long> readCapacity() {
        return Optional.empty();
    }

    /**
     * Specifies the write capacity
     */
    public Optional<Long> writeCapacity() {
        return Optional.empty();
    }

    /**
     * Interval (in minutes) how often to look for next chunk of jobs to schedule
     */
    public Optional<Long> interval() {
        return Optional.empty();
    }

    /**
     * Number of threads to be used for jobs execution
     */
    public Optional<Integer> threads() {
        return Optional.empty();
    }
}
