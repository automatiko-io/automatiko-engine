package io.automatiko.engine.api.config;

public class JobsConfig {

    /**
     * Configures file system based jobs service
     */
    public FileSystemJobsConfig filesystem() {
        return new FileSystemJobsConfig();
    }

    /**
     * Configures http based jobs service
     */
    public HttpJobsConfig http() {
        return new HttpJobsConfig();
    }

    /**
     * Configures database based jobs service
     */
    public DatabaseJobsConfig db() {
        return new DatabaseJobsConfig();
    }

    /**
     * Configures dynamodb based jobs service
     */
    public DynamoDBJobsConfig dynamodb() {
        return new DynamoDBJobsConfig();
    }

    /**
     * Configures cassandra based jobs service
     */
    public CassandraJobsConfig cassandra() {
        return new CassandraJobsConfig();
    }

    /**
     * Configures MongoDB based jobs service
     */
    public MongodbJobsConfig mongodb() {
        return new MongodbJobsConfig();
    }
}
