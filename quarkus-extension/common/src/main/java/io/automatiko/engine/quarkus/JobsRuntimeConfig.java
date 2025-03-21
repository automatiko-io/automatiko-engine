package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface JobsRuntimeConfig {

    /**
     * Configures jobs based on file system storage
     */
    FileSystemJobsRuntimeConfig filesystem();

    /**
     * Configures jobs based on database
     */
    DatabaseJobsRuntimeConfig db();

    /**
     * Configures jobs based on dynamodb
     */
    DynamoDBJobsRuntimeConfig dynamodb();

    /**
     * Configures jobs based on cassandra
     */
    CassandraJobsRuntimeConfig cassandra();

    /**
     * Configures jobs based on http endpoint
     */
    HttpJobsRuntimeConfig http();

    /**
     * Configures jobs based on mongodb
     */
    MongodbJobsRuntimeConfig mongodb();
}
