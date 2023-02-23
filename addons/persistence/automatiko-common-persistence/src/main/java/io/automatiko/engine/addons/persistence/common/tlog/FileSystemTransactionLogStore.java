package io.automatiko.engine.addons.persistence.common.tlog;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.uow.TransactionLogStore;
import io.quarkus.arc.DefaultBean;

@DefaultBean
@ApplicationScoped
public class FileSystemTransactionLogStore implements TransactionLogStore {

    private Path storage;

    public FileSystemTransactionLogStore(
            @ConfigProperty(name = "quarkus.automatiko.persistence.transaction-log.folder") Optional<String> transactionLogFolder) {

        if (transactionLogFolder.isPresent()) {
            storage = Paths.get(transactionLogFolder.get());
        }
    }

    @Override
    public void store(String transactionId, String processId, String instanceId, byte[] content) {
        Path path = Paths.get(storage.toString(), transactionId, processId, instanceId);
        try {
            Files.createDirectories(path.getParent());

            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] load(String processId, String instanceId) {
        String[] transactions = storage.toFile().list();
        try {
            if (transactions != null) {
                for (String transactionId : transactions) {
                    File transactionFolder = new File(storage.toFile(), transactionId);

                    File processIdFolder = new File(transactionFolder, processId);

                    String[] instances = processIdFolder.list((dir, name) -> name.equals(instanceId));

                    if (instances != null && instances.length == 1) {
                        return Files.readAllBytes(Paths.get(processIdFolder.getAbsolutePath(), instances[0]));
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<String> list(String processId) {
        String[] transactions = storage.toFile().list();
        Set<String> recoverable = new LinkedHashSet<>();
        if (transactions != null) {
            for (String transactionId : transactions) {
                File transactionFolder = new File(storage.toFile(), transactionId);

                File processIdFolder = new File(transactionFolder, processId);

                String[] instances = processIdFolder.list();
                if (instances != null) {
                    for (String instance : instances) {
                        recoverable.add(transactionId + "|" + instance);
                    }
                }
            }
        }
        return recoverable;
    }

    @Override
    public void delete(String transactionId) {
        Path path = Paths.get(storage.toString(), transactionId);
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(f -> {
                            f.delete();
                        });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String transactionId, String processId, String instanceId) {
        Path path = Paths.get(storage.toString(), transactionId, processId, instanceId);
        try {
            Files.deleteIfExists(path);

            Path processFolder = path.getParent();
            if (processFolder.toFile().list().length == 0) {
                Files.delete(processFolder);

                Path transactionFolder = processFolder.getParent();
                if (transactionFolder.toFile().list().length == 0) {
                    Files.delete(transactionFolder);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean contains(String processId, String instanceId) {
        String[] transactions = storage.toFile().list();
        if (transactions != null) {
            for (String transactionId : transactions) {
                File transactionFolder = new File(storage.toFile(), transactionId);

                File processIdFolder = new File(transactionFolder, processId);

                File instanceFolder = new File(processIdFolder, instanceId);

                if (instanceFolder.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<String> list() {
        if (storage == null) {
            return Collections.emptySet();
        }
        String[] transactions = storage.toFile().list();
        if (transactions == null) {
            return Collections.emptySet();
        }
        return Stream.of(transactions).collect(Collectors.toSet());
    }

}
