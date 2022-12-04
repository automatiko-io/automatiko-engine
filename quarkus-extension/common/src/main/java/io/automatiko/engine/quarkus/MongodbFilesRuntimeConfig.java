package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.MongodbFilesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class MongodbFilesRuntimeConfig extends MongodbFilesConfig {

    /**
     * Determines the name of database where files should be stored
     */
    @ConfigItem
    public Optional<String> database;

    /**
     * Specifies chunk size (in bytes) to be used when uploading files
     */
    @ConfigItem
    public Optional<Integer> chunkSize;

    @Override
    public Optional<String> database() {
        return database;
    }

    @Override
    public Optional<Integer> chunkSize() {
        return chunkSize;
    }

}
