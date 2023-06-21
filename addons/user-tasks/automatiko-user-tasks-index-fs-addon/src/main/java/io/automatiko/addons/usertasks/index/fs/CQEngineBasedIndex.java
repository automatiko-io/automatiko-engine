package io.automatiko.addons.usertasks.index.fs;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CQEngineBasedIndex {

    private IndexedCollection<CQEngineUserTaskInfo> tasks;

    private String path;

    public CQEngineBasedIndex(@ConfigProperty(name = "quarkus.automatiko.index.usertasks.fs.path") Optional<String> path) {
        this.path = path.orElse(System.getProperty("java.io.tmpdir")) + File.separator + "automatiko-user-tasks.dat";
        try {
            Files.createDirectories(Paths.get(this.path).getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.tasks = new ConcurrentIndexedCollection<>(
                DiskPersistence.onPrimaryKeyInFile(CQEngineUserTaskInfo.TASK_ID, new File(this.path)));
    }

    @PostConstruct
    public void setup() {

        tasks.addIndex(SuffixTreeIndex.onAttribute(CQEngineUserTaskInfo.TASK_NAME));
        tasks.addIndex(SuffixTreeIndex.onAttribute(CQEngineUserTaskInfo.TASK_DESCRIPTION));
        tasks.addIndex(HashIndex.onAttribute(CQEngineUserTaskInfo.POT_OWNERS));
        tasks.addIndex(HashIndex.onAttribute(CQEngineUserTaskInfo.POT_GROUPS));
        tasks.addIndex(HashIndex.onAttribute(CQEngineUserTaskInfo.EXCLUDED_USERS));
    }

    public IndexedCollection<CQEngineUserTaskInfo> get() {
        return tasks;
    }
}
