package io.automatiko.engine.addons.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import io.automatiko.engine.api.workflow.ProcessInstance;

public class Indexer {

    private Path indexFolder;

    public Indexer(Path persistenceFolder) {
        this.indexFolder = Paths.get(persistenceFolder.toString(), ".index");
        try {
            boolean indexExists = Files.exists(indexFolder);
            Files.createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ACTIVE)));
            Files.createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ERROR)));
            Files.createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ABORTED)));
            Files.createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_COMPLETED)));

            if (!indexExists) {
                reindex();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void index(ProcessInstance<?> instance) {
        index(instance.id(), instance.status(), instance.tags().values());
    }

    public void index(String id, int status, Collection<String> tags) {
        Path currentStatePath = Paths.get(indexFolder.toString(), String.valueOf(status), id);

        Set<String> info = new LinkedHashSet<>();
        info.add(id);
        info.addAll(tags);
        byte[] data = info.stream().map(value -> value + System.lineSeparator()).collect(Collectors.joining())
                .getBytes(StandardCharsets.UTF_8);
        try {

            Files.deleteIfExists(
                    Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ACTIVE), id));
            Files.deleteIfExists(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ERROR), id));

            Files.write(currentStatePath, data);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(String id, ProcessInstance<?> instance) {
        try {
            Files.deleteIfExists(
                    Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ACTIVE), id));
            Files.deleteIfExists(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ERROR), id));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<IndexedInstance> instances(int status, int page, int pageSize) {
        Path currentStatePath = Paths.get(indexFolder.toString(), String.valueOf(status));

        try {
            return Files.walk(currentStatePath, 1).skip(calculatePage(page, pageSize)).limit(pageSize)
                    .filter(file -> !Files.isDirectory(file))
                    .map(file -> new IndexedInstance(file.toFile().getName(), lines(file))).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptySet();

        }
    }

    protected Collection<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {

            e.printStackTrace();

            return Collections.emptySet();
        }
    }

    protected int calculatePage(int page, int size) {
        if (page <= 1) {
            return 0;
        }

        return (page - 1) * size;
    }

    private void reindex() {
        Path folder = this.indexFolder.getParent();
        try {
            Files.walk(folder, 2).filter(file -> isValidFile(file)).forEach(file -> {

                String id = file.toFile().getName();
                int status = Integer
                        .valueOf(getMetadata(file, FileSystemProcessInstances.PI_STATUS));
                Collection<String> piTags = new HashSet<>();
                String tags = getMetadata(file, FileSystemProcessInstances.PI_TAGS);

                if (tags != null) {
                    piTags.addAll(Arrays.asList(tags.split(",")));
                }

                index(id, status, piTags);

            });
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    protected boolean isValidFile(Path file) {
        try {
            return !Files.isDirectory(file) && !Files.isHidden(file);
        } catch (IOException e) {

            return false;
        }
    }

    protected String getMetadata(Path file, String key) {

        if (supportsUserDefinedAttributes(file)) {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
            try {
                ByteBuffer bb = ByteBuffer.allocate(view.size(key));
                view.read(key, bb);
                bb.flip();
                return Charset.defaultCharset().decode(bb).toString();
            } catch (IOException e) {
                return null;
            }
        } else {
            return getDotFileMetadata(file.toFile()).get(key);
        }

    }

    protected boolean supportsUserDefinedAttributes(Path file) {
        try {
            return Files.getFileStore(file).supportsFileAttributeView(UserDefinedFileAttributeView.class);
        } catch (IOException e) {
            return false;
        }
    }

    /*
     * fallback mechanism based on .file.metadata to keep user defined info
     */

    @SuppressWarnings("unchecked")
    protected Map<String, String> getDotFileMetadata(File file) {
        try (FileInputStream in = new FileInputStream(new File(file.getParent(), "._metadata_" + file.getName()))) {
            Properties props = new Properties();

            props.load(in);

            return (Map) props;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }
}
