package io.automatiko.engine.api.config;

public class FilesConfig {

    public FileSystemFilesConfig fs() {
        return new FileSystemFilesConfig() {
        };
    }

    public S3FilesConfig s3() {
        return new S3FilesConfig() {
        };
    }

    public GoogleStorageFilesConfig googleStorage() {
        return new GoogleStorageFilesConfig() {
        };
    }

    public MongodbFilesConfig mongodb() {
        return new MongodbFilesConfig() {
        };
    }
}
