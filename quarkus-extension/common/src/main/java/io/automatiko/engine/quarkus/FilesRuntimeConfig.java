package io.automatiko.engine.quarkus;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface FilesRuntimeConfig {

    /**
     * Provides configuration of file system based files
     */
    FileSystemFilesRuntimeConfig fs();

    /**
     * Provides configuration of AWS S3 based files
     */
    S3FilesRuntimeConfig s3();

    /**
     * Provides configuration of Google Storage based files
     */
    GoogleStorageFilesRuntimeConfig googleStorage();

    /**
     * Provides configuration of MongoDB (GridFS) based files
     */
    MongodbFilesRuntimeConfig mongodb();

    /**
     * Provides configuration of Azure BlobStore based files
     */
    AzureFilesRuntimeConfig azure();

}
