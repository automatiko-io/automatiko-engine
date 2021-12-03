package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.CassandraJobsConfig;
import io.automatiko.engine.api.config.DatabaseJobsConfig;
import io.automatiko.engine.api.config.DynamoDBJobsConfig;
import io.automatiko.engine.api.config.FileSystemJobsConfig;
import io.automatiko.engine.api.config.HttpJobsConfig;
import io.automatiko.engine.api.config.JobsConfig;
import io.automatiko.engine.api.config.MongodbJobsConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JobsBuildTimeConfig extends JobsConfig {

    /**
     * Determines the type of persistence to be used
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Configures jobs based on file system storage
     */
    @ConfigItem
    public FileSystemJobsBuildTimeConfig filesystem;

    /**
     * Configures jobs based on database
     */
    @ConfigItem
    public DatabaseJobsBuildTimeConfig db;

    /**
     * Configures jobs based on dynamodb
     */
    @ConfigItem
    public DynamoDBJobsBuildTimeConfig dynamodb;

    /**
     * Configures jobs based on cassandra
     */
    @ConfigItem
    public CassandraJobsBuildTimeConfig cassandra;

    /**
     * Configures jobs based on http endpoint
     */
    @ConfigItem
    public HttpJobsBuildTimeConfig http;

    /**
     * Configures jobs based on mongodb
     */
    @ConfigItem
    public MongodbJobsBuildTimeConfig mongodb;

    @Override
    public Optional<String> type() {
        return type;
    }

    @Override
    public FileSystemJobsConfig filesystem() {
        return filesystem;
    }

    @Override
    public HttpJobsConfig http() {
        return http;
    }

    @Override
    public DatabaseJobsConfig db() {
        return db;
    }

    @Override
    public DynamoDBJobsConfig dynamodb() {
        return dynamodb;
    }

    @Override
    public CassandraJobsConfig cassandra() {
        return cassandra;
    }

    @Override
    public MongodbJobsConfig mongodb() {
        return mongodb;
    }
}
