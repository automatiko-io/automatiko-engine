package io.automatiko.engine.api.config;

import java.util.Optional;

public class MongodbJobsConfig {

    /**
     * Name of the data base to be used to create collections for jobs
     */
    public Optional<String> database() {
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
