package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.FileSystemFilesConfig;
import io.automatiko.engine.api.config.FilesConfig;
import io.automatiko.engine.api.config.GoogleStorageFilesConfig;
import io.automatiko.engine.api.config.S3FilesConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FilesBuildTimeConfig extends FilesConfig {

    /**
     * Provides configuration of file system based files
     */
    @ConfigItem
    public FileSystemFilesBuildTimeConfig fs;

    /**
     * Provides configuration of AWS S3 based files
     */
    @ConfigItem
    public S3FilesBuildTimeConfig s3;

    /**
     * Provides configuration of Google Storage based files
     */
    @ConfigItem
    public GoogleStorageFilesBuildTimeConfig googleStorage;

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

}
