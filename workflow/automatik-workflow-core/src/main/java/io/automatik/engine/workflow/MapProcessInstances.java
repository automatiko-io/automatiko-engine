
package io.automatik.engine.workflow;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceDuplicatedException;

class MapProcessInstances<T> implements MutableProcessInstances<T> {

	private final ConcurrentHashMap<String, ProcessInstance<T>> instances = new ConcurrentHashMap<>();

	@Override
	public Optional<? extends ProcessInstance<T>> findById(String id) {
		return Optional.ofNullable(instances.get(resolveId(id)));
	}

	@Override
	public Collection<? extends ProcessInstance<T>> values() {
		return instances.values();
	}

	@Override
	public void create(String id, ProcessInstance<T> instance) {
		if (isActive(instance)) {
			ProcessInstance<T> existing = instances.putIfAbsent(resolveId(id), instance);
			if (existing != null) {
				throw new ProcessInstanceDuplicatedException(id);
			}
		}
	}

	@Override
	public void update(String id, ProcessInstance<T> instance) {
		if (isActive(instance)) {
			instances.put(resolveId(id), instance);
		}
	}

	@Override
	public void remove(String id) {
		instances.remove(resolveId(id));
	}

	@Override
	public boolean exists(String id) {
		return instances.containsKey(id);
	}

}
