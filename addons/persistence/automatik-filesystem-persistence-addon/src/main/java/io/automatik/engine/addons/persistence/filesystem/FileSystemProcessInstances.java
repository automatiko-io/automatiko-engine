
package io.automatik.engine.addons.persistence.filesystem;

import static io.automatik.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "rawtypes" })
public class FileSystemProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemProcessInstances.class);

    public static final String PI_DESCRIPTION = "ProcessInstanceDescription";
    public static final String PI_STATUS = "ProcessInstanceStatus";
    public static final String PI_ROOT_INSTANCE = "RootProcessInstance";
    public static final String PI_SUB_INSTANCE_COUNT = "SubProcessInstanceCount";
    public static final String PI_TAGS = "ProcessInstanceTags";

    private boolean useCompositeIdForSubprocess = true;

    private Process<?> process;
    private Path storage;

    private ProcessInstanceMarshaller marshaller;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public FileSystemProcessInstances(Process<?> process, Path storage) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy()));
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, boolean useCompositeIdForSubprocess) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy()),
                useCompositeIdForSubprocess);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller) {
        this(process, storage, marshaller, true);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller,
            boolean useCompositeIdForSubprocess) {
        this.process = process;
        this.storage = Paths.get(storage.toString(), process.id());
        this.marshaller = marshaller;
        this.useCompositeIdForSubprocess = useCompositeIdForSubprocess;

        try {
            Files.createDirectories(this.storage);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create directories for file based storage of process instances", e);
        }
    }

    public Integer size() {
        try (Stream<Path> stream = Files.walk(storage)) {
            Long count = stream.filter(file -> isValidProcessFile(file)).count();
            return count.intValue();
        } catch (IOException e) {
            throw new RuntimeException("Unable to count process instances ", e);
        }
    }

    @Override
    public Optional findById(String id, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return Optional.of(cachedInstances.get(resolvedId));
        }

        Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

        if (Files.notExists(processInstanceStorage)) {
            return Optional.empty();
        }
        byte[] data = readBytesFromFile(processInstanceStorage);
        return Optional.of(mode == MUTABLE ? marshaller.unmarshallProcessInstance(data, process)
                : marshaller.unmarshallReadOnlyProcessInstance(data, process));

    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        List collected = new ArrayList<>();
        for (String idOrTag : values) {

            findById(idOrTag, mode).ifPresent(pi -> collected.add(pi));

            try (Stream<Path> stream = Files.walk(storage)) {
                stream.filter(file -> isValidProcessFile(file))
                        .filter(file -> matchTag(getMetadata(file, PI_TAGS), idOrTag))
                        .map(this::readBytesFromFile)
                        .map(b -> mode == MUTABLE ? marshaller.unmarshallProcessInstance(b, process)
                                : marshaller.unmarshallReadOnlyProcessInstance(b, process))
                        .forEach(pi -> collected.add(pi));
            } catch (IOException e) {
                throw new RuntimeException("Unable to read process instances ", e);
            }
        }
        return collected;
    }

    @Override
    public Collection values(ProcessInstanceReadMode mode) {
        try (Stream<Path> stream = Files.walk(storage)) {
            return stream.filter(file -> isValidProcessFile(file)).map(this::readBytesFromFile)
                    .map(b -> mode == MUTABLE ? marshaller.unmarshallProcessInstance(b, process)
                            : marshaller.unmarshallReadOnlyProcessInstance(b, process))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read process instances ", e);
        }
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(Paths.get(storage.toString(), resolveId(id)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void create(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance)) {

            Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

            if (Files.exists(processInstanceStorage)) {
                throw new ProcessInstanceDuplicatedException(id);
            }
            storeProcessInstance(processInstanceStorage, instance);
            cachedInstances.remove(resolvedId);
            cachedInstances.remove(id);
        } else if (isPending(instance)) {
            if (cachedInstances.putIfAbsent(resolvedId, instance) != null) {
                throw new ProcessInstanceDuplicatedException(id);
            }
        } else {
            cachedInstances.remove(resolvedId);
            cachedInstances.remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance)) {

            Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

            if (Files.exists(processInstanceStorage)) {
                storeProcessInstance(processInstanceStorage, instance);
            }
        }
        cachedInstances.remove(resolvedId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);
        Path processInstanceMetadataStorage = Paths.get(storage.toString(), "._metadata_" + resolveId(id));
        cachedInstances.remove(resolvedId);
        cachedInstances.remove(id);
        try {
            Files.deleteIfExists(processInstanceStorage);

            Files.deleteIfExists(processInstanceMetadataStorage);
        } catch (IOException e) {
            throw new RuntimeException("Unable to remove process instance with id " + id, e);
        }

    }

    @Override
    public boolean useCompositeIdForSubprocess() {

        return useCompositeIdForSubprocess;
    }

    protected void storeProcessInstance(Path processInstanceStorage, ProcessInstance<?> instance) {
        try {
            byte[] data = marshaller.marhsallProcessInstance(instance);
            Files.write(processInstanceStorage, data);
            setMetadata(processInstanceStorage, PI_DESCRIPTION, instance.description());
            setMetadata(processInstanceStorage, PI_STATUS, String.valueOf(instance.status()));

            if (instance.parentProcessInstanceId() == null) {
                setMetadata(processInstanceStorage, PI_ROOT_INSTANCE, "true");
            } else {
                setMetadata(processInstanceStorage, PI_ROOT_INSTANCE, "false");
            }
            setMetadata(processInstanceStorage, PI_SUB_INSTANCE_COUNT, String.valueOf(instance.subprocesses().size()));
            setMetadata(processInstanceStorage, PI_TAGS, instance.tags().values().stream().collect(Collectors.joining(",")));

            disconnect(processInstanceStorage, instance);
        } catch (IOException e) {
            throw new RuntimeException("Unable to store process instance with id " + instance.id(), e);
        }
    }

    protected byte[] readBytesFromFile(Path processInstanceStorage) {
        try {
            return Files.readAllBytes(processInstanceStorage);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read process instance from " + processInstanceStorage, e);
        }
    }

    protected boolean isValidProcessFile(Path file) {

        try {
            return !Files.isDirectory(file) && !Files.isHidden(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void disconnect(Path processInstanceStorage, ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

            try {
                byte[] reloaded = readBytesFromFile(processInstanceStorage);

                return marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }

        });
    }

    protected boolean matchTag(String metadata, String idOrTag) {
        if (metadata != null) {
            return Stream.of(metadata.split(",")).anyMatch(item -> item.equals(idOrTag));
        }
        return false;
    }

    public String getMetadata(Path file, String key) {

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

    public boolean setMetadata(Path file, String key, String value) {

        if (supportsUserDefinedAttributes(file)) {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
            try {
                if (value != null) {
                    view.write(key, Charset.defaultCharset().encode(value));
                } else {
                    view.delete(key);
                }
                return true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return setDotFileMetadata(file.toFile(), key, value);
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

    protected boolean setDotFileMetadata(File file, String key, String value) {
        File metadataDotFile = new File(file.getParent(), "._metadata_" + file.getName());
        if (!metadataDotFile.exists()) {
            try {
                metadataDotFile.createNewFile();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try (FileInputStream in = new FileInputStream(metadataDotFile)) {
            Properties props = new Properties();

            props.load(in);
            if (value == null) {
                props.remove(key);
            } else {
                props.setProperty(key, value);
            }
            try (FileOutputStream out = new FileOutputStream(metadataDotFile)) {
                props.store(out, "");
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
