package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface MongodbJobsRuntimeConfig {

    /**
     * Name of the data base to be used to create collections for jobs
     */
    Optional<String> database();

    /**
     * Interval (in minutes) how often to look for next chunk of jobs to schedule
     */
    Optional<Long> interval();

    /**
     * Number of threads to be used for jobs execution
     */
    Optional<Integer> threads();

}
