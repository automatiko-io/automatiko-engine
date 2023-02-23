package io.automatiko.addon.files.mongodb;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.BsonString;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;

@ApplicationScoped
public class GridFSStore {

    private String serviceUrl;

    private String database;

    private Integer chunkSize;

    private StoredDataCodec codec;

    private GridFSBucket gridFSBucket;

    @Inject
    public GridFSStore(StoredDataCodec codec, MongoClient mongoClient,
            @ConfigProperty(name = "quarkus.automatiko.files.mongodb.database") Optional<String> database,
            @ConfigProperty(name = "quarkus.automatiko.files.mongodb.chunk-size") Optional<Integer> chunkSize,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.codec = codec;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080") + "/management/files/download/";
        this.database = database.orElse("automatiko");
        this.chunkSize = chunkSize.orElse(1048576);
        this.gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase(this.database));
    }

    public String urlPrefix() {
        return serviceUrl;
    }

    public void save(byte[] content, String processId, String processVersion, String processInstanceId, String... name) {

        Document metadata = new Document("processId", processId).append("processVersion", processVersion)
                .append("processInstanceId", processInstanceId);

        if (name.length >= 2) {
            metadata.append("variable", name[0]).append("filename", name[1]);
        }
        try (InputStream streamToUploadFrom = new ByteArrayInputStream(codec.encode(content))) {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(this.chunkSize)
                    .metadata(metadata);
            String fileId = buildId(processId, processVersion, processInstanceId, name);

            gridFSBucket.uploadFromStream(new BsonString(fileId), buildName(processId, processVersion, processInstanceId, name),
                    streamToUploadFrom,
                    options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void replace(byte[] content, String processId, String processVersion, String processInstanceId, String... name) {
        remove(processId, processVersion, processInstanceId, name);

        save(content, processId, processVersion, processInstanceId, name);
    }

    public void remove(String processId, String processVersion, String processInstanceId, String... name) {

        String fileId = buildId(processId, processVersion, processInstanceId, name);

        if (gridFSBucket.find(Filters.eq("_id", fileId)).first() != null) {
            gridFSBucket.delete(new BsonString(fileId));
        }

    }

    public byte[] content(String url) {
        String items = url.replaceFirst(serviceUrl, "");
        BsonString fileId = new BsonString(UUID.nameUUIDFromBytes(items.getBytes(StandardCharsets.UTF_8)).toString());

        try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(fileId)) {

            int fileLength = (int) downloadStream.getGridFSFile().getLength();
            byte[] content = new byte[fileLength];
            downloadStream.read(content);

            return codec.decode(content);
        }
    }

    public byte[] content(String processId, String processVersion, String processInstanceId, String... name)
            throws FileNotFoundException {
        BsonString fileId = new BsonString(buildId(processId, processVersion, processInstanceId, name));

        try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(fileId)) {
            int fileLength = (int) downloadStream.getGridFSFile().getLength();
            byte[] content = new byte[fileLength];
            downloadStream.read(content);

            return codec.decode(content);
        }
    }

    protected String buildName(String processId, String processVersion, String processInstanceId, String... name) {
        List<String> elements = new ArrayList<>();
        elements.add(processId);
        if (processVersion != null && !processVersion.isEmpty()) {
            elements.add(processVersion);
        }
        elements.add(processInstanceId);

        for (String nameElement : name) {
            elements.add(nameElement);
        }

        return Stream.of(elements.toArray(String[]::new)).collect(Collectors.joining("/"));
    }

    protected String buildId(String processId, String processVersion, String processInstanceId, String... name) {

        String idAsPath = buildName(processId, processVersion, processInstanceId, name);

        return UUID.nameUUIDFromBytes(idAsPath.getBytes(StandardCharsets.UTF_8)).toString();
    }

}
