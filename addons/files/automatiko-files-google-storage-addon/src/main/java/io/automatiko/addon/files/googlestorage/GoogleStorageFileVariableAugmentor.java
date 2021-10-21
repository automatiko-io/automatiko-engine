package io.automatiko.addon.files.googlestorage;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.automatiko.engine.api.workflow.Variable;
import io.automatiko.engine.api.workflow.VariableAugmentor;
import io.automatiko.engine.workflow.file.ByteArrayFile;

@ApplicationScoped
public class GoogleStorageFileVariableAugmentor implements VariableAugmentor {

    private String serviceUrl;

    private GoogleStorageStore store;

    @Inject
    public GoogleStorageFileVariableAugmentor(GoogleStorageStore store) {
        this.store = store;
        this.serviceUrl = store.urlPrefix();
    }

    @Override
    public boolean accept(Variable variable, Object value) {
        if (value == null) {
            // check variable definition
            if (variable.getType().getClassType() != null) {
                return ByteArrayFile.class.isAssignableFrom(variable.getType().getClassType());
            }
        }

        if (value instanceof ByteArrayFile) {
            return true;
        }

        if (value instanceof Collection) {
            return ((Collection<?>) value).stream().anyMatch(item -> item instanceof ByteArrayFile);
        }
        return false;
    }

    @Override
    public Object augmentOnCreate(String processId, String processVersion, String processInstanceId, Variable variable,
            Object value) {
        if (value == null) {
            return value;
        }
        StringBuilder url = new StringBuilder(serviceUrl);
        url.append(processId).append("/");
        if (processVersion != null && !processVersion.isEmpty()) {
            url.append(processVersion).append("/");
        }
        url.append(processInstanceId).append("/").append(variable.getName());

        if (value instanceof ByteArrayFile) {
            ByteArrayFile file = (ByteArrayFile) value;

            GoogleStorageFile fsFile = new GoogleStorageFile(file.name(), null, file.attributes());
            fsFile.url(url.toString() + "/" + file.name());

            // store file on file system
            store.save(file, processId, processVersion, processInstanceId, variable.getName(),
                    file.name());
            value = fsFile;
        } else if (value instanceof Collection) {
            Collection<GoogleStorageFile> fsFiles = new ArrayList<>();
            for (Object potentialFile : (Collection<?>) value) {
                if (potentialFile instanceof ByteArrayFile) {
                    ByteArrayFile file = (ByteArrayFile) potentialFile;
                    GoogleStorageFile fsFile = new GoogleStorageFile(file.name(), null, file.attributes());
                    fsFile.url(url.toString() + "/" + file.name());

                    // store file on file system
                    store.save(file, processId, processVersion, processInstanceId, variable.getName(),
                            file.name());
                    fsFiles.add(fsFile);
                }
            }
            return fsFiles;
        }

        return value;
    }

    @Override
    public Object augmentOnUpdate(String processId, String processVersion, String processInstanceId, Variable variable,
            Object value) {
        if (value == null) {
            return value;
        }
        StringBuilder url = new StringBuilder(serviceUrl);
        url.append(processId).append("/");
        if (processVersion != null && !processVersion.isEmpty()) {
            url.append(processVersion).append("/");
        }
        url.append(processInstanceId).append("/").append(variable.getName());

        if (value instanceof ByteArrayFile) {
            ByteArrayFile file = (ByteArrayFile) value;
            GoogleStorageFile fsFile = new GoogleStorageFile(file.name(), null, file.attributes());
            fsFile.url(url.toString() + "/" + file.name());

            // replace file on file system
            store.replace(file, processId, processVersion, processInstanceId, variable.getName(),
                    file.name());
            value = fsFile;
        } else if (value instanceof Collection) {
            Collection<GoogleStorageFile> fsFiles = new ArrayList<>();
            for (Object potentialFile : (Collection<?>) value) {
                if (potentialFile instanceof ByteArrayFile) {
                    ByteArrayFile file = (ByteArrayFile) potentialFile;
                    GoogleStorageFile fsFile = new GoogleStorageFile(file.name(), null, file.attributes());
                    fsFile.url(url.toString() + "/" + file.name());

                    // replace file on file system
                    store.replace(file, processId, processVersion, processInstanceId, variable.getName(),
                            file.name());
                    fsFiles.add(fsFile);
                }
            }
            return fsFiles;
        }

        return value;
    }

    @Override
    public void augmentOnDelete(String processId, String processVersion, String processInstanceId, Variable variable,
            Object value) {

        if (value instanceof ByteArrayFile) {
            ByteArrayFile file = (ByteArrayFile) value;
            store.remove(processId, processVersion, processInstanceId, variable.getName(), file.name());
        } else if (value instanceof Collection) {
            for (Object potentialFile : (Collection<?>) value) {
                if (potentialFile instanceof ByteArrayFile) {
                    ByteArrayFile file = (ByteArrayFile) potentialFile;
                    store.remove(processId, processVersion, processInstanceId, variable.getName(), file.name());
                }
            }

        }

    }

}
