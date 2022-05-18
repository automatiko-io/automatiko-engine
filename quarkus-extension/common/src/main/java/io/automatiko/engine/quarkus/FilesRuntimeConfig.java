package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.FileSystemFilesConfig;
import io.automatiko.engine.api.config.FilesConfig;
import io.automatiko.engine.api.config.GoogleStorageFilesConfig;
import io.automatiko.engine.api.config.MongodbFilesConfig;
import io.automatiko.engine.api.config.S3FilesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FilesRuntimeConfig extends FilesConfig {

    /**
     * Provides configuration of file system based files
     */
    @ConfigItem
    public FileSystemFilesRuntimeConfig fs;

    /**
     * Provides configuration of AWS S3 based files
     */
    @ConfigItem
    public S3FilesRuntimeConfig s3;

    /**
     * Provides configuration of Google Storage based files
     */
    @ConfigItem
    public GoogleStorageFilesRuntimeConfig googleStorage;

    /**
     * Provides configuration of MongoDB (GridFS) based files
     */
    @ConfigItem
    public MongodbFilesRuntimeConfig mongodb;

    @Override
    public FileSystemFilesConfig fs() {
        return fs;
    }

    @Override
    public S3FilesConfig s3() {
        return s3;
    }

    @Override
    public GoogleStorageFilesConfig googleStorage() {
        return googleStorage;
    }

    @Override
    public MongodbFilesConfig mongodb() {
        return mongodb;
    }

}
