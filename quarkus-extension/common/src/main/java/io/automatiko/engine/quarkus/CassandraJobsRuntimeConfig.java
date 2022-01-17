package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.CassandraJobsConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.jobs.cassandra")
public class CassandraJobsRuntimeConfig extends CassandraJobsConfig {

    /**
     * Indicates if keyspace should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createKeyspace;

    /**
     * Indicates if tables should be created during startup
     */
    @ConfigItem
    public Optional<Boolean> createTables;

    /**
     * Specifies if keyspace name to be used if not given it defaults to application name
     */
    @ConfigItem
    public Optional<String> keyspace;

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
    public Optional<Boolean> createKeyspace() {
        return createKeyspace;
    }

    @Override
    public Optional<Boolean> createTables() {
        return createTables;
    }

    @Override
    public Optional<String> keyspace() {
        return keyspace;
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
