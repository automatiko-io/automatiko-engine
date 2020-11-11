
package io.automatik.engine.addons.persistence.infinispan;

import static io.automatik.engine.api.workflow.ProcessInstanceReadMode.MUTABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.MessageMarshaller;

import io.automatik.engine.api.auth.AccessDeniedException;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "rawtypes" })
public class CacheProcessInstances implements MutableProcessInstances {

    private final RemoteCache<String, byte[]> cache;
    private ProcessInstanceMarshaller marshaller;

    private io.automatik.engine.api.workflow.Process<?> process;

    private Map<String, ProcessInstance> cachedInstances = new ConcurrentHashMap<>();

    public CacheProcessInstances(Process<?> process, RemoteCacheManager cacheManager, String templateName, String proto,
            MessageMarshaller<?>... marshallers) {
        this.process = process;
        this.cache = cacheManager.administration().getOrCreateCache(process.id() + "_store",
                ignoreNullOrEmpty(templateName));

        this.marshaller = new ProcessInstanceMarshaller(new ProtoStreamObjectMarshallingStrategy(proto, marshallers));
    }

    public Integer size() {
        return cache.size();
    }

    @Override
    public Optional<? extends ProcessInstance> findById(String id, ProcessInstanceReadMode mode) {
        String resolvedId = resolveId(id);
        if (cachedInstances.containsKey(resolvedId)) {
            return Optional.of(cachedInstances.get(resolvedId));
        }

        byte[] data = cache.get(resolvedId);
        if (data == null) {
            return Optional.empty();
        }

        return Optional.of(mode == MUTABLE ? marshaller.unmarshallProcessInstance(data, process)
                : marshaller.unmarshallReadOnlyProcessInstance(data, process));
    }

    @Override
    public Collection<? extends ProcessInstance> findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        List<ProcessInstance> collected = new ArrayList<>();
        for (String idOrTag : values) {

            cache.values().parallelStream()
                    .map(data -> {
                        try {
                            return mode == MUTABLE ? marshaller.unmarshallProcessInstance(data, process)
                                    : marshaller.unmarshallReadOnlyProcessInstance(data, process);
                        } catch (AccessDeniedException e) {
                            return null;
                        }
                    })
                    .filter(pi -> pi != null)
                    .filter(pi -> {
                        if (pi.id().equals(resolveId(idOrTag)) || pi.tags().values().contains(idOrTag)) {
                            return true;
                        } else {
                            pi.disconnect();
                            return false;
                        }
                    })
                    .forEach(pi -> collected.add(pi));
        }
        return collected;
    }

    @Override
    public Collection<? extends ProcessInstance> values(ProcessInstanceReadMode mode, int page, int size) {
        return cache.values().parallelStream()
                .map(data -> {
                    try {
                        return mode == MUTABLE ? marshaller.unmarshallProcessInstance(data, process)
                                : marshaller.unmarshallReadOnlyProcessInstance(data, process);
                    } catch (AccessDeniedException e) {
                        return null;
                    }
                })
                .filter(pi -> pi != null)
                .skip(calculatePage(page, size))
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public void update(String id, ProcessInstance instance) {
        updateStorage(id, instance, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(String id, ProcessInstance instance) {
        String resolvedId = resolveId(id, instance);
        cache.remove(resolvedId);
        cachedInstances.remove(resolvedId);
        cachedInstances.remove(id);
    }

    protected String ignoreNullOrEmpty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value;
    }

    @Override
    public void create(String id, ProcessInstance instance) {
        updateStorage(id, instance, true);

    }

    @SuppressWarnings("unchecked")
    protected void updateStorage(String id, ProcessInstance instance, boolean checkDuplicates) {
        String resolvedId = resolveId(id, instance);
        if (isActive(instance)) {

            byte[] data = marshaller.marhsallProcessInstance(instance);

            if (checkDuplicates) {
                byte[] existing = cache.putIfAbsent(resolvedId, data);
                if (existing != null) {
                    throw new ProcessInstanceDuplicatedException(id);
                }
            } else if (cache.containsKey(resolvedId)) {
                cache.put(resolvedId, data);
            }

            ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
                byte[] reloaded = cache.get(resolvedId);
                if (reloaded != null) {
                    return marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
                }

                return null;
            });
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

    @Override
    public boolean exists(String id) {
        return cache.containsKey(id);
    }

}
