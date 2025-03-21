package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface MongodbFilesRuntimeConfig {

    /**
     * Determines the name of database where files should be stored
     */
    Optional<String> database();

    /**
     * Specifies chunk size (in bytes) to be used when uploading files
     */
    Optional<Integer> chunkSize();

}
