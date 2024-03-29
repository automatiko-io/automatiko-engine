
package io.automatiko.engine.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MapProcessInstances implements MutableProcessInstances {

    private final ConcurrentHashMap<String, ProcessInstance> instances = new ConcurrentHashMap<>();

    private final ProcessInstanceMarshaller marshaller = new ProcessInstanceMarshaller();

    @Override
    public TransactionLog transactionLog() {
        return null;
    }

    public Long size() {
        return (long) instances.size();
    }

    @Override
    public Optional<ProcessInstance> findById(String id, int status, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        ProcessInstance instance = instances.get(resolvedId);
        if (instance != null) {
            instance.process().accessPolicy().canReadInstance(IdentityProvider.get(), instance);

            if (instance.status() == status) {
                return Optional.ofNullable(instance);
            }
        }
        if (resolvedId.contains(":")) {
            if (instances.containsKey(resolvedId.split(":")[1])) {
                ProcessInstance pi = instances.get(resolvedId.split(":")[1]);
                if (pi.status() == status) {
                    return Optional.of(pi);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<ProcessInstance> values(ProcessInstanceReadMode mode, int status, int page, int size) {
        return instances.values().stream()
                .filter(pi -> pi.status() == status)
                .filter(pi -> pi.process().accessPolicy().canReadInstance(IdentityProvider.get(), pi))
                .skip(calculatePage(page, size))
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public void create(String id, ProcessInstance instance) {
        if (isActive(instance)) {
            ProcessInstance existing = instances.putIfAbsent(resolveId(id, instance), instance);
            if (existing != null) {
                throw new ProcessInstanceDuplicatedException(id);
            }
        }
    }

    @Override
    public void update(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance) && instances.containsKey(resolvedId)) {
            instances.put(resolvedId, instance);
        }
    }

    @Override
    public void remove(String id, ProcessInstance instance) {
        instances.remove(resolveId(id, instance));
    }

    @Override
    public boolean exists(String id) {
        return instances.containsKey(id);
    }

    @Override
    public Collection<? extends ProcessInstance> findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values) {
        List<ProcessInstance> collected = new ArrayList<>();
        for (String idOrTag : values) {

            instances.values().stream()
                    .filter(pi -> pi.id().equals(resolveId(idOrTag)) || pi.tags().values().contains(idOrTag))
                    .filter(pi -> pi.status() == status)
                    .filter(pi -> pi.process().accessPolicy().canReadInstance(IdentityProvider.get(), pi))
                    .forEach(pi -> collected.add(pi));
        }
        return collected;
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

    @Override
    public Collection locateByIdOrTag(int status, String... values) {

        return findByIdOrTag(ProcessInstanceReadMode.READ_ONLY, status, values).stream().map(pi -> pi.id())
                .collect(Collectors.toSet());
    }

}
