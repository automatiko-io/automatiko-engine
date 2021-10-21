package io.automatiko.addon.files.googlestorage;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

import io.automatiko.engine.workflow.file.ByteArrayFile;

@ApplicationScoped
public class GoogleStorageStore {

    private String serviceUrl;

    private String bucket;

    private Storage storage;

    @Inject
    public GoogleStorageStore(Storage storage,
            @ConfigProperty(name = "quarkus.automatiko.files.google-storage.bucket") String bucket,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.storage = storage;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080") + "/management/files/download/";
        this.bucket = bucket;
    }

    public String urlPrefix() {
        return serviceUrl;
    }

    public void save(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        Bucket gbucket = storage.get(bucket);

        gbucket.create(objectKey, file.content(), file.type());
    }

    public void replace(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {

        save(file, processId, processVersion, processInstanceId, name);
    }

    public void remove(String processId, String processVersion, String processInstanceId, String... name) {

        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        Bucket gbucket = storage.get(bucket);

        gbucket.get(objectKey).delete();
    }

    public byte[] content(String url) {
        String objectKey = url.replaceFirst(serviceUrl, "");

        Bucket gbucket = storage.get(bucket);

        return gbucket.get(objectKey).getContent();

    }

    public byte[] content(String processId, String processVersion, String processInstanceId, String... name)
            throws FileNotFoundException {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        Bucket gbucket = storage.get(bucket);

        return gbucket.get(objectKey).getContent();
    }

    protected String createObjectKey(String processId, String processVersion, String processInstanceId, String... name) {
        List<String> elements = new ArrayList<>();
        elements.add(processId);
        if (processVersion != null && !processVersion.isEmpty()) {
            elements.add(processVersion);
        }
        elements.add(processInstanceId);

        for (String nameElement : name) {
            elements.add(nameElement);
        }

        return elements.stream().collect(Collectors.joining("/"));
    }
}
