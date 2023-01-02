
package io.automatiko.engine.addons.persistence.filesystem;

import static io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy;
import io.automatiko.engine.addons.persistence.common.tlog.TransactionLogImpl;
import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.auth.AccessDeniedException;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.uow.TransactionLogStore;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.automatiko.engine.workflow.AbstractProcessInstance;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;
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
    public static final String PI_START_DATE = "ProcessInstanceStartDate";
    public static final String PI_END_DATE = "ProcessInstanceEndDate";
    public static final String PI_EXPIRED_AT_DATE = "ProcessInstanceExpiredAtDate";
    public static final String PI_BUSINESS_KEY = "ProcessInstanceKey";

    private static final int DEFAULT_LOCK_TIMEOUT = 60 * 1000;

    private static final int DEFAULT_LOCK_LIMIT = 5000;

    private static final int DEFAULT_LOCK_WAIT = 100;

    private boolean useCompositeIdForSubprocess = true;

    private Process<?> process;
    private Path storage;

    private ProcessInstanceMarshaller marshaller;

    private StoredDataCodec codec;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    private TransactionLog transactionLog;

    private Auditor auditor;

    private Indexer indexer;

    private int configuredLockTimeout = DEFAULT_LOCK_TIMEOUT;

    private int configuredLockLimit = DEFAULT_LOCK_LIMIT;

    private int configuredLockWait = DEFAULT_LOCK_WAIT;

    public FileSystemProcessInstances(Process<?> process, Path storage, StoredDataCodec codec, TransactionLogStore store,
            Auditor auditor, Optional<Integer> lockTimeout, Optional<Integer> lockLimit, Optional<Integer> lockWait) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy(process)), codec, store,
                auditor);
        this.configuredLockTimeout = lockTimeout.orElse(DEFAULT_LOCK_TIMEOUT);
        this.configuredLockLimit = lockLimit.orElse(DEFAULT_LOCK_LIMIT);
        this.configuredLockWait = lockWait.orElse(DEFAULT_LOCK_WAIT);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, boolean useCompositeIdForSubprocess,
            StoredDataCodec codec, TransactionLogStore store, Auditor auditor) {
        this(process, storage, new ProcessInstanceMarshaller(new JacksonObjectMarshallingStrategy(process)),
                useCompositeIdForSubprocess, codec, store, auditor);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller,
            StoredDataCodec codec, TransactionLogStore store, Auditor auditor) {
        this(process, storage, marshaller, true, codec, store, auditor);
    }

    public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller,
            boolean useCompositeIdForSubprocess, StoredDataCodec codec, TransactionLogStore store, Auditor auditor) {
        this.process = process;
        this.storage = Paths.get(storage.toString(), process.id());
        this.marshaller = marshaller;
        this.useCompositeIdForSubprocess = useCompositeIdForSubprocess;
        this.codec = codec;
        this.auditor = auditor;

        try {
            Files.createDirectories(this.storage);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create directories for file based storage of process instances", e);
        }
        LOGGER.debug("Location of the file system process storage is {}", storage);

        this.transactionLog = new TransactionLogImpl(store, new JacksonObjectMarshallingStrategy(process));
        this.indexer = new Indexer(this.storage);
    }

    @Override
    public TransactionLog transactionLog() {
        return this.transactionLog;
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

        if (status == ProcessInstance.STATE_RECOVERING) {
            byte[] content = this.transactionLog.readContent(process.id(), resolvedId);
            long versionTracker = 1;
            if (Files.exists(processInstanceStorage)) {
                versionTracker = getVersionTracker(processInstanceStorage);
            }
            return Optional.of(audit(
                    mode == MUTABLE ? marshaller.unmarshallProcessInstance(content, process, versionTracker)
                            : marshaller.unmarshallReadOnlyProcessInstance(content, process)));
        }

        if (Files.notExists(processInstanceStorage)
                || Integer.parseInt(getMetadata(processInstanceStorage, PI_STATUS)) != status) {
            return Optional.empty();
        }
        byte[] data;

        switch (mode) {
            case MUTABLE:
                data = readBytesFromFile(processInstanceStorage);
                return Optional
                        .of(marshaller.unmarshallProcessInstance(data, process, getVersionTracker(processInstanceStorage)));
            case MUTABLE_WITH_LOCK:
                acquireLock(resolvedId);
                try {
                    data = readBytesFromFile(processInstanceStorage);
                    return Optional
                            .of(marshaller.unmarshallProcessInstance(data, process, getVersionTracker(processInstanceStorage)));
                } catch (Throwable e) {
                    releaseLock(resolvedId);
                }
            default:
                data = readBytesFromFile(processInstanceStorage);
                return Optional.of(marshaller.unmarshallReadOnlyProcessInstance(data, process));

        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values) {
        Set collected = new LinkedHashSet<>();

        Set<String> found = indexer.instances(status, 0, Integer.MAX_VALUE).stream()
                .filter(instance -> instance.match(values)).map(instance -> instance.id()).collect(Collectors.toSet());

        for (String id : found) {
            try {
                findById(id, status, mode).ifPresent(pi -> collected.add(pi));
            } catch (AccessDeniedException e) {

            }

        }
        return collected;
    }

    @Override
    public Collection findByIdOrTag(ProcessInstanceReadMode mode, int status, String sortBy, boolean sortAsc,
            String... values) {
        String sortKey = adjustSortKey(sortBy);
        if (sortKey == null) {
            return findByIdOrTag(mode, status, values);
        }

        Set collected = new LinkedHashSet<>();

        Set<String> found = indexer.instances(status, 0, Integer.MAX_VALUE).stream()
                .filter(instance -> instance.match(values)).map(instance -> instance.id()).collect(Collectors.toSet());
        List<SortItem> sortingValues = new ArrayList<>();
        if (sortKey.equals("id")) {
            for (String id : found) {
                sortingValues.add(new SortItem(id, id));
            }
        } else {
            for (String id : found) {
                Path processInstanceStorage = Paths.get(storage.toString(), id);
                String metadataSort = getMetadata(processInstanceStorage, sortKey);
                sortingValues.add(new SortItem(metadataSort, id));
            }
        }

        sortingValues.sort((one, two) -> {
            return one.key.compareTo(two.key);
        });

        if (!sortAsc) {
            Collections.reverse(sortingValues);
        }

        for (SortItem item : sortingValues) {
            try {
                findById(item.id, status, mode).ifPresent(pi -> collected.add(pi));
            } catch (AccessDeniedException e) {

            }

        }
        return collected;
    }

    @Override
    public Collection locateByIdOrTag(int status, String... values) {
        Set<String> collected = indexer.instances(status, 0, Integer.MAX_VALUE).stream()
                .filter(instance -> instance.match(values)).map(instance -> instance.id()).collect(Collectors.toSet());
        return collected;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size) {
        Set collected = new LinkedHashSet<>();

        Set<String> found = indexer.instances(status, page, size).stream()
                .map(instance -> instance.id()).collect(Collectors.toSet());

        for (String id : found) {
            try {
                findById(id, status, mode).ifPresent(pi -> collected.add(pi));
            } catch (AccessDeniedException e) {

            }
        }
        return collected;
    }

    @Override
    public Collection values(ProcessInstanceReadMode mode, int status, int page, int size, String sortBy, boolean sortAsc) {
        String sortKey = adjustSortKey(sortBy);
        if (sortKey == null) {
            return values(mode, status, page, size);
        }

        Set collected = new LinkedHashSet<>();

        Set<String> found = indexer.instances(status, 0, Integer.MAX_VALUE).stream()
                .map(instance -> instance.id()).collect(Collectors.toSet());
        List<SortItem> sortingValues = new ArrayList<>();
        if (sortKey.equals("id")) {
            found.stream().skip(calculatePage(page, size)).limit(size).forEach(itemId -> {
                sortingValues.add(new SortItem(itemId, itemId));
            });

        } else {
            found.stream().skip(calculatePage(page, size)).limit(size).forEach(itemId -> {
                Path processInstanceStorage = Paths.get(storage.toString(), itemId);
                String metadataSort = getMetadata(processInstanceStorage, sortKey);
                sortingValues.add(new SortItem(metadataSort, itemId));
            });
        }

        sortingValues.sort((one, two) -> {
            return one.key.compareTo(two.key);
        });

        if (!sortAsc) {
            Collections.reverse(sortingValues);
        }

        for (SortItem item : sortingValues) {
            try {
                findById(item.id, status, mode).ifPresent(pi -> collected.add(pi));
            } catch (AccessDeniedException e) {

            }

        }
        return collected;
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
            acquireLock(resolvedId);
            cachedInstances.remove(resolvedId);
            cachedInstances.remove(id);

            storeProcessInstance(resolvedId, processInstanceStorage, instance);

            Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                    .add("message", "Workflow instance created in the file system based data store");

            auditor.publish(entry);

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

            if (Files.exists(processInstanceStorage) || transactionLog.contains(process.id(), resolvedId)) {

                storeProcessInstance(resolvedId, processInstanceStorage, instance);
                Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                        .add("message", "Workflow instance updated in the file system based data store");

                auditor.publish(entry);
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

            indexer.remove(resolvedId, instance);

            releaseLock(resolvedId);

            Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                    .add("message", "Workflow instance deleted from the file system based data store");

            auditor.publish(entry);
        } catch (IOException e) {
            throw new RuntimeException("Unable to remove process instance with id " + id, e);
        }

    }

    @Override
    public boolean useCompositeIdForSubprocess() {

        return useCompositeIdForSubprocess;
    }

    protected void storeProcessInstance(String resolvedId, Path processInstanceStorage, ProcessInstance<?> instance) {
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
            setMetadata(processInstanceStorage, PI_BUSINESS_KEY, instance.businessKey());
            setMetadata(processInstanceStorage, PI_STATUS, String.valueOf(instance.status()));

            if (instance.parentProcessInstanceId() == null) {
                setMetadata(processInstanceStorage, PI_ROOT_INSTANCE, "true");
            } else {
                setMetadata(processInstanceStorage, PI_ROOT_INSTANCE, "false");
            }
            setMetadata(processInstanceStorage, PI_SUB_INSTANCE_COUNT, String.valueOf(instance.subprocesses().size()));
            setMetadata(processInstanceStorage, PI_TAGS, instance.tags().values().stream().collect(Collectors.joining(",")));

            setMetadata(processInstanceStorage, PI_START_DATE,
                    DateTimeFormatter.ISO_INSTANT.format(instance.startDate().toInstant()));
            if (instance.status() == ProcessInstance.STATE_ABORTED || instance.status() == ProcessInstance.STATE_COMPLETED) {
                setMetadata(processInstanceStorage, PI_END_DATE,
                        DateTimeFormatter.ISO_INSTANT.format(instance.endDate().toInstant()));
                Date expiration = instance.expiresAtDate();

                if (expiration != null) {
                    setMetadata(processInstanceStorage, PI_EXPIRED_AT_DATE,
                            DateTimeFormatter.ISO_INSTANT.format(expiration.toInstant()));
                }
            }

            indexer.index(resolvedId, instance.status(), instance.businessKey(), instance.tags().values());

            disconnect(processInstanceStorage, instance);
        } catch (IOException e) {
            throw new RuntimeException("Unable to store process instance with id " + instance.id(), e);
        } finally {
            releaseLock(resolvedId);
        }
    }

    @Override
    public void release(String id, ProcessInstance pi) {
        releaseLock(resolveId(id, pi));
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
            return !Files.isDirectory(file) && !Files.isHidden(file) && !file.toString().contains(".index");

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

        ExportedProcessInstance exported = marshaller.exportProcessInstance(audit(instance));

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

    public ProcessInstance<?> audit(ProcessInstance<?> instance) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.persitenceWrite(instance)
                .add("message", "Workflow instance was read from the file system based data store");

        auditor.publish(entry);

        return instance;
    }

    protected void acquireLock(String id) {

        Path processInstanceLock = Paths.get(storage.toString(), "." + id + ".lock");

        try {
            Files.createFile(processInstanceLock);

        } catch (FileAlreadyExistsException e) {
            try {
                try {
                    Files.createFile(processInstanceLock);

                } catch (FileAlreadyExistsException e2) {
                    if (Files.exists(processInstanceLock)) {
                        synchronized (this) {
                            if (Files.exists(processInstanceLock)) {
                                long value = System.currentTimeMillis() - configuredLockTimeout;
                                // first check if the lock file is not overdue

                                if (Files.getLastModifiedTime(processInstanceLock).toMillis() < value) {
                                    // if so recreate it
                                    Files.delete(processInstanceLock);
                                    Files.createFile(processInstanceLock);
                                    return;
                                }
                            }
                        }
                    }

                    // in case lock is not overdue then wait for it to be removed
                    int limit = 0;
                    while (true) {
                        if (limit > configuredLockLimit) {
                            throw new IllegalStateException(
                                    "Unable to aquire lock on process instance (" + id + ") within " + limit + " ms");
                        }

                        try {
                            Thread.sleep(configuredLockWait);
                        } catch (InterruptedException ex) {
                        }

                        limit += configuredLockWait;
                        try {
                            Files.createFile(processInstanceLock);
                            break;
                        } catch (FileAlreadyExistsException ex) {

                        }
                    }
                }

            } catch (IOException e1) {

                throw new UncheckedIOException(e1);
            }
        } catch (IOException e1) {
            throw new UncheckedIOException(e1);
        }

    }

    protected void releaseLock(String resolvedId) {
        try {
            Path processInstanceLock = Paths.get(storage.toString(), "." + resolvedId + ".lock");
            Files.deleteIfExists(processInstanceLock);
        } catch (IOException e1) {
            throw new UncheckedIOException(e1);
        }
    }

    protected String adjustSortKey(String sortBy) {
        switch (sortBy) {
            case ID_SORT_KEY:
                return "id";
            case DESC_SORT_KEY:
                return PI_DESCRIPTION;
            case START_DATE_SORT_KEY:
                return PI_START_DATE;
            case END_DATE_SORT_KEY:
                return PI_END_DATE;
            case BUSINESS_KEY_SORT_KEY:
                return PI_BUSINESS_KEY;
            default:
                return null;
        }
    }

    private class SortItem {
        public SortItem(String metadataSort, String id) {
            this.key = metadataSort;
            this.id = id;
        }

        private String key;

        private String id;
    }
}
