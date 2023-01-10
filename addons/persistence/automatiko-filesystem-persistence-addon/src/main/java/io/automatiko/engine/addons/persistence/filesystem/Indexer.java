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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstances;

public class Indexer {

    private Path indexFolder;

    private Set<SortableItem> activeInstance = ConcurrentHashMap.newKeySet();
    private Set<SortableItem> completedInstance = ConcurrentHashMap.newKeySet();
    private Set<SortableItem> abortedInstance = ConcurrentHashMap.newKeySet();
    private Set<SortableItem> inErrorInstance = ConcurrentHashMap.newKeySet();

    public Indexer(Path persistenceFolder) {

        this.indexFolder = Paths.get(persistenceFolder.toString(), ".index");
        try {
            boolean indexExists = Files.exists(indexFolder);
            Path activeDir = Files
                    .createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ACTIVE)));
            Path inErrorDir = Files
                    .createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ERROR)));
            Path abortedDir = Files
                    .createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_ABORTED)));
            Path completedDir = Files
                    .createDirectories(Paths.get(indexFolder.toString(), String.valueOf(ProcessInstance.STATE_COMPLETED)));

            if (!indexExists) {
                reindex();
            }

            // load sortable collections
            Files.list(activeDir).filter(path -> isValidFile(path)).forEach(path -> {
                SortableItem sortableItem = buildSortableItem(persistenceFolder, path);
                if (sortableItem != null) {
                    activeInstance.add(sortableItem);
                }

            });
            Files.list(inErrorDir).filter(path -> isValidFile(path)).forEach(path -> {
                SortableItem sortableItem = buildSortableItem(persistenceFolder, path);
                if (sortableItem != null) {
                    inErrorInstance.add(sortableItem);
                }

            });
            Files.list(abortedDir).filter(path -> isValidFile(path)).forEach(path -> {
                SortableItem sortableItem = buildSortableItem(persistenceFolder, path);
                if (sortableItem != null) {
                    abortedInstance.add(sortableItem);
                }

            });
            Files.list(completedDir).filter(path -> isValidFile(path)).forEach(path -> {
                SortableItem sortableItem = buildSortableItem(persistenceFolder, path);
                if (sortableItem != null) {
                    completedInstance.add(sortableItem);
                }

            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void index(ProcessInstance<?> instance) {
        index(instance.id(), instance.status(), instance.businessKey(), instance.tags().values(), instance);
    }

    public void index(String id, int status, String businessKey, Collection<String> tags, ProcessInstance<?> instance) {
        Path currentStatePath = Paths.get(indexFolder.toString(), String.valueOf(status), id);

        Set<String> info = new LinkedHashSet<>();
        info.add(id);
        if (businessKey != null) {
            info.add(businessKey);
        }
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
        if (instance != null) {
            // update sortable instances collections
            SortableItem sortableInstance = new SortableItem(id, instance.description(), businessKey, instance.startDate(),
                    instance.endDate(), tags);
            // always remove from in error
            inErrorInstance.remove(sortableInstance);
            switch (status) {
                case ProcessInstance.STATE_ACTIVE:
                    activeInstance.remove(sortableInstance);
                    activeInstance.add(sortableInstance);
                    break;
                case ProcessInstance.STATE_COMPLETED:
                    activeInstance.remove(sortableInstance);
                    completedInstance.add(sortableInstance);
                    break;
                case ProcessInstance.STATE_ABORTED:
                    activeInstance.remove(sortableInstance);
                    abortedInstance.add(sortableInstance);
                    break;
                case ProcessInstance.STATE_ERROR:
                    activeInstance.remove(sortableInstance);
                    inErrorInstance.add(sortableInstance);
                    break;
                default:
                    break;
            }
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

    public Collection<IndexedInstance> instances(int status, int page, int pageSize) {
        Path currentStatePath = Paths.get(indexFolder.toString(), String.valueOf(status));

        try {
            return Files.walk(currentStatePath, 1).skip(calculatePage(page, pageSize)).limit(pageSize)
                    .filter(file -> !Files.isDirectory(file))
                    .map(file -> new IndexedInstance(file.toFile().getName(), lines(file))).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptySet();

        }
    }

    public Collection<IndexedInstance> instances(int status, int page, int pageSize, String sortBy, boolean sortAsc) {

        List<SortableItem> instances;

        switch (status) {
            case ProcessInstance.STATE_ABORTED:
                instances = new ArrayList<>(abortedInstance);
                break;
            case ProcessInstance.STATE_COMPLETED:
                instances = new ArrayList<>(completedInstance);
                break;
            case ProcessInstance.STATE_ERROR:
                instances = new ArrayList<>(inErrorInstance);
                break;
            default:
                instances = new ArrayList<>(activeInstance);
                break;
        }

        instances.sort(determineComparator(sortBy, sortAsc));

        return instances.stream().skip(calculatePage(page, pageSize)).limit(pageSize)
                .map(file -> new IndexedInstance(file.id, file.tags)).collect(Collectors.toList());

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

                index(id, status, null, piTags, null);

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

        if (supportsUserDefinedAttributes(file) && !dotFileMetadataExists(file)) {
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

    public Map<String, String> getAllMetadata(Path file) {
        if (supportsUserDefinedAttributes(file) && !dotFileMetadataExists(file)) {

            UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);

            Map<String, String> attributes = new HashMap<String, String>();
            try {
                for (String key : view.list()) {
                    java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(view.size(key));
                    view.read(key, bb);
                    bb.flip();
                    attributes.put(key, Charset.defaultCharset().decode(bb).toString());
                }
            } catch (IOException e) {
                return getDotFileMetadata(file.toFile());
            }
            return attributes;

        } else {
            return getDotFileMetadata(file.toFile());
        }
    }

    /*
     * fallback mechanism based on .file.metadata to keep user defined info
     */

    protected boolean dotFileMetadataExists(Path instancePath) {

        File file = instancePath.toFile();
        File metadataDotFile = new File(file.getParent(), "._metadata_" + file.getName());

        return metadataDotFile.exists();
    }

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

    private SortableItem buildSortableItem(Path persistenceFolder, Path path) {
        String id = path.getName(path.getNameCount() - 1).toString();
        Path instancePath = Paths.get(persistenceFolder.toString(), id);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            Map<String, String> metadata = getAllMetadata(instancePath);

            String strStartDate = metadata.get(FileSystemProcessInstances.PI_START_DATE);
            String strEndDate = metadata.get(FileSystemProcessInstances.PI_END_DATE);

            Date startDate = strStartDate != null ? sdf.parse(strStartDate.toString()) : null;

            Date endDate = strEndDate != null ? sdf.parse(strEndDate.toString()) : null;

            String tags = metadata.get(FileSystemProcessInstances.PI_TAGS);

            SortableItem sortableItem = new SortableItem(id,
                    metadata.get(FileSystemProcessInstances.PI_DESCRIPTION),
                    metadata.get(FileSystemProcessInstances.PI_BUSINESS_KEY), startDate, endDate,
                    tags != null ? Arrays.asList(tags.split(",")) : new ArrayList<>());
            return sortableItem;

        } catch (ParseException e) {
        }

        return null;
    }

    private class SortableItem {

        private String id;
        private String description;
        private String businessKey;
        private Date startDate;
        private Date endDate;
        private Collection<String> tags;

        public SortableItem(String id, String description, String businessKey, Date startDate, Date endDate,
                Collection<String> tags) {
            this.id = id;
            this.description = description;
            this.businessKey = businessKey;
            this.startDate = startDate;
            this.endDate = endDate;
            this.tags = tags;
        }

        @Override
        public String toString() {
            return "SortableItem [id=" + id + ", description=" + description + ", businessKey=" + businessKey + ", startDate="
                    + startDate + ", endDate=" + endDate + ", tags=" + tags + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SortableItem other = (SortableItem) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        private Indexer getEnclosingInstance() {
            return Indexer.this;
        }
    }

    protected Comparator<SortableItem> determineComparator(String sortBy, boolean sortAsc) {
        switch (sortBy) {
            case ProcessInstances.ID_SORT_KEY:
                return sortAsc ? BY_ID_ASC : BY_ID_DESC;
            case ProcessInstances.DESC_SORT_KEY:
                return sortAsc ? BY_DESCRIPTION_ASC : BY_DESCRIPTION_DESC;
            case ProcessInstances.START_DATE_SORT_KEY:
                return sortAsc ? BY_START_DATE_ASC : BY_START_DATE_DESC;
            case ProcessInstances.END_DATE_SORT_KEY:
                return sortAsc ? BY_START_END_ASC : BY_START_END_DESC;
            case ProcessInstances.BUSINESS_KEY_SORT_KEY:
                return sortAsc ? BY_BUSINESS_KEY_ASC : BY_BUSINESS_KEY_DESC;
            default:
                return BY_START_DATE_ASC;
        }
    }

    private static Comparator<SortableItem> BY_START_DATE_ASC = (one, two) -> {
        if (one.startDate != null && two.startDate != null) {
            return one.startDate.compareTo(two.startDate);
        } else if (one.startDate != null && two.startDate == null) {
            return -1;
        } else if (one.startDate == null && two.startDate != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_START_DATE_DESC = (one, two) -> {
        if (one.startDate != null && two.startDate != null) {
            return two.startDate.compareTo(one.startDate);
        } else if (one.startDate != null && two.startDate == null) {
            return -1;
        } else if (one.startDate == null && two.startDate != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_START_END_ASC = (one, two) -> {
        if (one.endDate != null && two.endDate != null) {
            return one.endDate.compareTo(two.endDate);
        } else if (one.endDate != null && two.endDate == null) {
            return -1;
        } else if (one.endDate == null && two.endDate != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_START_END_DESC = (one, two) -> {
        if (one.endDate != null && two.endDate != null) {
            return two.endDate.compareTo(one.endDate);
        } else if (one.endDate != null && two.endDate == null) {
            return -1;
        } else if (one.endDate == null && two.endDate != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_ID_ASC = (one, two) -> {
        if (one.id != null && two.id != null) {
            return one.id.compareTo(two.id);
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_ID_DESC = (one, two) -> {
        if (one.id != null && two.id != null) {
            return two.id.compareTo(one.id);
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_DESCRIPTION_ASC = (one, two) -> {
        if (one.description != null && two.description != null) {
            return one.description.compareTo(two.description);
        } else if (one.description != null && two.description == null) {
            return -1;
        } else if (one.description == null && two.description != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_DESCRIPTION_DESC = (one, two) -> {
        if (one.description != null && two.description != null) {
            return two.description.compareTo(one.description);
        } else if (one.description != null && two.description == null) {
            return -1;
        } else if (one.description == null && two.description != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_BUSINESS_KEY_ASC = (one, two) -> {
        if (one.businessKey != null && two.businessKey != null) {
            return one.businessKey.compareTo(two.businessKey);
        } else if (one.businessKey != null && two.businessKey == null) {
            return -1;
        } else if (one.businessKey == null && two.businessKey != null) {
            return 1;
        } else {
            return -1;
        }
    };

    private static Comparator<SortableItem> BY_BUSINESS_KEY_DESC = (one, two) -> {
        if (one.businessKey != null && two.businessKey != null) {
            return two.businessKey.compareTo(one.businessKey);
        } else if (one.businessKey != null && two.businessKey == null) {
            return -1;
        } else if (one.businessKey == null && two.businessKey != null) {
            return 1;
        } else {
            return -1;
        }
    };
}
