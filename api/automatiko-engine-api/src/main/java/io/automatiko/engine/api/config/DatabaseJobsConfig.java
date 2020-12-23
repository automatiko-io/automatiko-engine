package io.automatiko.engine.api.config;

import java.util.Optional;

public class DatabaseJobsConfig {

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
