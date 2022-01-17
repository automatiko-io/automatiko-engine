package io.automatiko.engine.quarkus;

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
public class JobsRuntimeConfig extends JobsConfig {

    /**
     * Configures jobs based on file system storage
     */
    @ConfigItem
    public FileSystemJobsRuntimeConfig filesystem;

    /**
     * Configures jobs based on database
     */
    @ConfigItem
    public DatabaseJobsRuntimeConfig db;

    /**
     * Configures jobs based on dynamodb
     */
    @ConfigItem
    public DynamoDBJobsRuntimeConfig dynamodb;

    /**
     * Configures jobs based on cassandra
     */
    @ConfigItem
    public CassandraJobsRuntimeConfig cassandra;

    /**
     * Configures jobs based on http endpoint
     */
    @ConfigItem
    public HttpJobsRuntimeConfig http;

    /**
     * Configures jobs based on mongodb
     */
    @ConfigItem
    public MongodbJobsRuntimeConfig mongodb;

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
