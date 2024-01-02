package io.automatiko.addon.files.s3;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AzureBlobStoreStore {

    private String serviceUrl;

    private String bucket;

    private BlobServiceClient client;

    @Inject
    public AzureBlobStoreStore(BlobServiceClient client,
            @ConfigProperty(name = "quarkus.automatiko.files.azure.bucket") String bucket,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.client = client;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080") + "/management/files/download/";
        this.bucket = bucket;
    }

    public String urlPrefix() {
        return serviceUrl;
    }

    public void save(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        BlobContainerClient blobContainerClient = client.createBlobContainerIfNotExists(this.bucket);
        BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

        blobClient.upload(BinaryData.fromBytes(file.content()), true);
    }

    public void replace(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {

        save(file, processId, processVersion, processInstanceId, name);
    }

    public void remove(String processId, String processVersion, String processInstanceId, String... name) {

        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        BlobContainerClient blobContainerClient = client.createBlobContainerIfNotExists(this.bucket);
        BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);
        blobClient.delete();
    }

    public byte[] content(String url) {
        String objectKey = url.replaceFirst(serviceUrl, "");

        BlobContainerClient blobContainerClient = client.createBlobContainerIfNotExists(bucket);
        BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

        try {
            byte[] content = blobClient.downloadContent().toBytes();

            return content;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public byte[] content(String processId, String processVersion, String processInstanceId, String... name)
            throws FileNotFoundException {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        BlobContainerClient blobContainerClient = client.createBlobContainerIfNotExists(bucket);
        BlobClient blobClient = blobContainerClient.getBlobClient(objectKey);

        try {
            byte[] content = blobClient.downloadContent().toBytes();

            return content;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
