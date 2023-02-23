package io.automatiko.engine.quarkus.strategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.ArchiveStore;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
@DefaultBean
public class FileSystemArchiveStore implements ArchiveStore {

    private Optional<String> location;

    public FileSystemArchiveStore(@ConfigProperty(name = "quarkus.automatiko.archive-path") Optional<String> location) {
        this.location = location;
        if (this.location.isEmpty()) {
            throw new IllegalStateException(
                    "Archive path property is required for archiving workflow instances, specify it via 'quarkus.automatiko.archive-path' property");
        }
    }

    @Override
    public void store(ArchivedProcessInstance archivedInstance) {
        File file = new File(
                location.get() + File.separator + archivedInstance.getProcessId(), archivedInstance.getId() + ".zip");
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {

            archivedInstance.writeAsZip(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
