
package io.automatiko.engine.addons.persistence.filesystem;

import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "rawtypes" })
public class FileSystemProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemProcessInstances.class);

    public static final String PI_DESCRIPTION = "ProcessInstanceDescription";
    public static final String PI_STATUS = "ProcessInstanceStatus";
    public static final String PI_ROOT_INSTANCE = "RootProcessInstance";
    public static final String PI_SUB_INSTANCE_COUNT = "SubProcessInstanceCount";
    public static final String PI_TAGS = "ProcessInstanceTags";
    public static final String PI_VERSION = "ProcessInstanceVersion";

    private boolean useCompositeIdForSubprocess = true;

    private Process<?> process;
    private Path storage;

    private ProcessInstanceMarshaller marshaller;

    private StoredDataCodec codec;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public FileSystemProcessInstances(Process<?> process, Path storage, StoredDataCodec codec) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy()), codec);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, boolean useCompositeIdForSubprocess,
            StoredDataCodec codec) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy()),
                useCompositeIdForSubprocess, codec);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller,
            StoredDataCodec codec) {
        this(process, storage, marshaller, true, codec);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller,
            boolean useCompositeIdForSubprocess, StoredDataCodec codec) {
        this.process = process;
        this.storage = Paths.get(storage.toString(), process.id());
        this.marshaller = marshaller;
        this.useCompositeIdForSubprocess = useCompositeIdForSubprocess;
        this.codec = codec;

        try {
            Files.createDirectories(this.storage);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create directories for file based storage of process instances", e);
        }
        LOGGER.info("Location of the storage is {}", storage);
    }

    public Long size() {
        try (Stream<Path> stream = Files.walk(storage)) {
            Long count = stream.filter(file -> isValidProcessFile(file)).count();
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Unable to count process instances ", e);
        }
    }

    @Override
    public Optional findById(String id, int status, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            ProcessInstance pi = cachedInstances.get(resolvedId);
            if (pi.status() == status) {
                return Optional.of(pi);
            } else {
                return Optional.empty();
            }
        }
        if (resolvedId.contains(":")) {
            if (cachedInstances.containsKey(resolvedId.split(":")[1])) {
                ProcessInstance pi = cachedInstances.get(resolvedId.split(":")[1]);
                if (pi.status() == status) {
                    return Optional.of(pi);
                } else {
                    return Optional.empty();
                }
            }
        }

        Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

        if (Files.notExists(processInstanceStorage)
                || Integer.parseInt(getMetadata(processInstanceStorage, PI_STATUS)) != status) {
            return Optional.empty();
        }
        byte[] data = readBytesFromFile(processInstanceStorage);
        return Optional.of(
                mode == MUTABLE ? marshaller.unmarshallProcessInstance(data, process, getVersionTracker(processInstanceStorage))
                        : marshaller.unmarshallReadOnlyProcessInstance(data, process));

    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values) {
        Set collected = new LinkedHashSet<>();
        for (String idOrTag : values) {

            findById(idOrTag, mode).ifPresent(pi -> collected.add(pi));

            try (Stream<Path> stream = Files.walk(storage)) {
                stream.filter(file -> isValidProcessFile(file, status))
                        .filter(file -> matchTag(getMetadata(file, PI_TAGS), idOrTag))
                        .map(file -> {
                            try {
                                byte[] b = readBytesFromFile(file);
                                return mode == MUTABLE
                                        ? marshaller.unmarshallProcessInstance(b, process, getVersionTracker(file))
                                        : marshaller.unmarshallReadOnlyProcessInstance(b, process);
                            } catch (AccessDeniedException e) {
                                return null;
                            }
                        })
                        .filter(pi -> pi != null)
                        .forEach(pi -> collected.add(pi));
            } catch (IOException e) {
                throw new RuntimeException("Unable to read process instances ", e);
            }
        }
        return collected;
    }

    @Override
    public Collection locateByIdOrTag(int status, String... values) {
        Set<String> collected = new LinkedHashSet<>();
        for (String idOrTag : values) {

            try (Stream<Path> stream = Files.walk(storage)) {
                stream.filter(file -> isValidProcessFile(file, status))
                        .filter(file -> matchTag(getMetadata(file, PI_TAGS), idOrTag))
                        .forEach(file -> collected.add(file.getName(file.getNameCount() - 1).toString()));
            } catch (IOException e) {
                throw new RuntimeException("Unable to read process instances ", e);
            }
        }
        return collected;
    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size) {
        try (Stream<Path> stream = Files.walk(storage)) {
            return stream.filter(file -> isValidProcessFile(file, status))
                    .map(file -> {
                        try {
                            byte[] b = readBytesFromFile(file);
                            return mode == MUTABLE ? marshaller.unmarshallProcessInstance(b, process, getVersionTracker(file))
                                    : marshaller.unmarshallReadOnlyProcessInstance(b, process);
                        } catch (AccessDeniedException e) {
                            return null;
                        }
                    })
                    .filter(pi -> pi != null)
                    .skip(calculatePage(page, size))
                    .limit(size)
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

            cachedInstances.remove(resolvedId);
            cachedInstances.remove(id);

            storeProcessInstance(processInstanceStorage, instance);
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
        cachedInstances.remove(resolvedId);
        if (isActive(instance)) {

            Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

            if (Files.exists(processInstanceStorage)) {
                storeProcessInstance(processInstanceStorage, instance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);
        Path processInstanceMetadataStorage = Paths.get(storage.toString(), "._metadata_" + resolvedId);
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
            byte[] data = codec.encode(marshaller.marhsallProcessInstance(instance));
            if (data == null) {
                return;
            }
            long storedVersion = getVersionTracker(processInstanceStorage);
            long instanceVersion = ((AbstractProcessInstance<?>) instance).getVersionTracker();

            if (storedVersion != instanceVersion) {
                throw new ConflictingVersionException("Process instance with id '" + instance.id()
                        + "' has older version than the stored one (" + instanceVersion + " != " + storedVersion + ")");
            }
            // first store the version of the instance for conflict tracking
            setMetadata(processInstanceStorage, PI_VERSION, String.valueOf(instanceVersion + 1));
            // then store the instance and other metadata
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
            return codec.decode(Files.readAllBytes(processInstanceStorage));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read process instance from " + processInstanceStorage, e);
        }
    }

    protected boolean isValidProcessFile(Path file, int status) {

        try {
            boolean valid = !Files.isDirectory(file) && !Files.isHidden(file);

            if (!valid) {
                return false;
            }
            String statusMetadata = getMetadata(file, PI_STATUS);
            if (statusMetadata != null && Integer.parseInt(statusMetadata) == status) {
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    @Override
    public ExportedProcessInstance exportInstance(ProcessInstance instance, boolean abort) {

        ExportedProcessInstance exported = marshaller.exportProcessInstance(instance);

        if (abort) {
            instance.abort();
        }

        return exported;

    }

    @Override
    public ProcessInstance importInstance(ExportedProcessInstance instance, Process process) {
        ProcessInstance imported = marshaller.importProcessInstance(instance, process);

        if (exists(imported.id())) {
            throw new ProcessInstanceDuplicatedException(imported.id());
        }

        create(imported.id(), imported);
        return imported;
    }

    protected long getVersionTracker(Path file) {
        String version = getMetadata(file, PI_VERSION);
        if (version == null) {
            return 1;
        }

        return Long.parseLong(version);
    }

}
