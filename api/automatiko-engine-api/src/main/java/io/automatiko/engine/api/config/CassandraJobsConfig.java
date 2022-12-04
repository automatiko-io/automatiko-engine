package io.automatiko.engine.api.config;

import java.util.Optional;

public class CassandraJobsConfig {
    public static final String CREATE_KEYSPACE_KEY = "quarkus.automatiko.jobs.cassandra.create-keyspace";
    public static final String CREATE_TABLES_KEY = "quarkus.automatiko.jobs.cassandra.create-tables";
    public static final String KEYSPACE_KEY = "quarkus.automatiko.jobs.cassandra.keyspace";
    public static final String INTERVAL_KEY = "quarkus.automatiko.jobs.cassandra.interval";
    public static final String THREADS_KEY = "quarkus.automatiko.jobs.cassandra.threads";

    /**
     * Indicates if keyspace should be created during startup.
     */
    public Optional<Boolean> createKeyspace() {
        return Optional.empty();
    }

    /**
     * Indicates if tables should be created during startup.
     */
    public Optional<Boolean> createTables() {
        return Optional.empty();
    }

    /**
     * Specifies if keyspace name to be used if not given it defaults to 'automatiko'
     */
    public Optional<String> keyspace() {
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
