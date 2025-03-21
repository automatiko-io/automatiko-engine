package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DatabaseJobsRuntimeConfig {

    /**
     * Interval (in minutes) how often to look for next chunk of jobs to schedule
     */
    Optional<Long> interval();

    /**
     * Number of threads to be used for jobs execution
     */
    Optional<Integer> threads();
}
