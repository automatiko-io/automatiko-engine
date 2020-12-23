
package io.automatiko.engine.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MapProcessInstances implements MutableProcessInstances {

    private final ConcurrentHashMap<String, ProcessInstance> instances = new ConcurrentHashMap<>();

    public Integer size() {
        return instances.size();
    }

    @Override
    public Optional<ProcessInstance> findById(String id, ProcessInstanceReadMode mode) {
        ProcessInstance instance = instances.get(resolveId(id));
        if (instance != null) {
            instance.process().accessPolicy().canReadInstance(IdentityProvider.get(), instance);
        }
        return Optional.ofNullable(instance);
    }

    @Override
    public Collection<ProcessInstance> values(ProcessInstanceReadMode mode, int page, int size) {
        return instances.values().stream().filter(pi -> pi.process().accessPolicy().canReadInstance(IdentityProvider.get(), pi))
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
    public Collection<? extends ProcessInstance> findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        List<ProcessInstance> collected = new ArrayList<>();
        for (String idOrTag : values) {

            instances.values().stream().filter(pi -> pi.id().equals(resolveId(idOrTag)) || pi.tags().values().contains(idOrTag))
                    .filter(pi -> pi.process().accessPolicy().canReadInstance(IdentityProvider.get(), pi))
                    .forEach(pi -> collected.add(pi));
        }
        return collected;
    }

}
