
package io.automatik.engine.addons.persistence.infinispan;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.MessageMarshaller;

import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;
import io.automatik.engine.workflow.AbstractProcessInstance;
import io.automatik.engine.workflow.marshalling.ProcessInstanceMarshaller;

@SuppressWarnings({ "rawtypes" })
public class CacheProcessInstances implements MutableProcessInstances {

	private final RemoteCache<String, byte[]> cache;
	private ProcessInstanceMarshaller marshaller;

	private io.automatik.engine.api.workflow.Process<?> process;

	public CacheProcessInstances(Process<?> process, RemoteCacheManager cacheManager, String templateName, String proto,
			MessageMarshaller<?>... marshallers) {
		this.process = process;
		this.cache = cacheManager.administration().getOrCreateCache(process.id() + "_store",
				ignoreNullOrEmpty(templateName));

		this.marshaller = new ProcessInstanceMarshaller(new ProtoStreamObjectMarshallingStrategy(proto, marshallers));
	}

	@Override
	public Optional<? extends ProcessInstance> findById(String id) {
		byte[] data = cache.get(resolveId(id));
		if (data == null) {
			return Optional.empty();
		}

		return Optional.of(marshaller.unmarshallProcessInstance(data, process));
	}

	@Override
	public Collection<? extends ProcessInstance> values() {
		return cache.values().parallelStream().map(data -> marshaller.unmarshallProcessInstance(data, process))
				.collect(Collectors.toList());
	}

	@Override
	public void update(String id, ProcessInstance instance) {
		updateStorage(id, instance, false);
	}

	@Override
	public void remove(String id) {
		cache.remove(resolveId(id));
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
		if (isActive(instance)) {
			String resolvedId = resolveId(id);
			byte[] data = marshaller.marhsallProcessInstance(instance);

			if (checkDuplicates) {
				byte[] existing = cache.putIfAbsent(resolvedId, data);
				if (existing != null) {
					throw new ProcessInstanceDuplicatedException(id);
				}
			} else {
				cache.put(resolvedId, data);
			}

			((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
				byte[] reloaded = cache.get(resolvedId);
				if (reloaded != null) {
					return ((AbstractProcessInstance<?>) marshaller.unmarshallProcessInstance(reloaded, process,
							(AbstractProcessInstance<?>) instance)).internalGetProcessInstance();
				}

				return null;
			});
		}
	}

	@Override
	public boolean exists(String id) {
		return cache.containsKey(id);
	}
}
