package io.automatiko.addon.files.s3;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ApplicationScoped
public class S3Store {

    private String serviceUrl;

    private String bucket;

    private S3Client s3;

    @Inject
    public S3Store(S3Client s3, @ConfigProperty(name = "quarkus.automatiko.files.s3.bucket") String bucket,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.s3 = s3;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080") + "/management/files/download/";
        this.bucket = bucket;
    }

    public String urlPrefix() {
        return serviceUrl;
    }

    public void save(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.type())
                .build();

        s3.putObject(putRequest, RequestBody.fromBytes(file.content()));
    }

    public void replace(ByteArrayFile file, String processId, String processVersion, String processInstanceId,
            String... name) {

        save(file, processId, processVersion, processInstanceId, name);
    }

    public void remove(String processId, String processVersion, String processInstanceId, String... name) {

        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        s3.deleteObject(deleteRequest);
    }

    public byte[] content(String url) {
        String objectKey = url.replaceFirst(serviceUrl, "");

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            GetObjectResponse object = s3.getObject(getRequest, ResponseTransformer.toOutputStream(baos));

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public byte[] content(String processId, String processVersion, String processInstanceId, String... name)
            throws FileNotFoundException {
        String objectKey = createObjectKey(processId, processVersion, processInstanceId, name);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            GetObjectResponse object = s3.getObject(getRequest, ResponseTransformer.toOutputStream(baos));

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
