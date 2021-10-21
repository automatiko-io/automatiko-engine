package io.automatiko.addon.files.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;

@ApplicationScoped
public class FileStore {

    private String serviceUrl;

    private String store;

    private StoredDataCodec codec;

    @Inject
    public FileStore(StoredDataCodec codec, @ConfigProperty(name = "quarkus.automatiko.files.fs.location") String location,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.codec = codec;
        this.serviceUrl = serviceUrl.orElse("http://localhost:8080") + "/management/files/download/";
        this.store = location;
    }

    public String urlPrefix() {
        return serviceUrl;
    }

    public void save(byte[] content, String processId, String processVersion, String processInstanceId, String... name) {
        Path path = createPath(processId, processVersion, processInstanceId, name);

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, codec.encode(content));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void replace(byte[] content, String processId, String processVersion, String processInstanceId, String... name) {
        remove(processId, processVersion, processInstanceId, name[0]);

        save(content, processId, processVersion, processInstanceId, name);
    }

    public void remove(String processId, String processVersion, String processInstanceId, String... name) {

        Path path = createPath(processId, processVersion, processInstanceId, name);

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] content(String url) {
        String items = url.replaceFirst(serviceUrl, "");

        Path path = Paths.get(store, items.split("/"));

        if (Files.exists(path)) {
            try {
                return codec.decode(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return null;
        }
    }

    public byte[] content(String processId, String processVersion, String processInstanceId, String... name)
            throws FileNotFoundException {
        Path path = createPath(processId, processVersion, processInstanceId, name);

        if (Files.exists(path)) {
            try {
                return codec.decode(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    protected Path createPath(String processId, String processVersion, String processInstanceId, String... name) {
        List<String> elements = new ArrayList<>();
        elements.add(processId);
        if (processVersion != null && !processVersion.isEmpty()) {
            elements.add(processVersion);
        }
        elements.add(processInstanceId);

        for (String nameElement : name) {
            elements.add(nameElement);
        }

        return Paths.get(store, elements.toArray(String[]::new));
    }
}
