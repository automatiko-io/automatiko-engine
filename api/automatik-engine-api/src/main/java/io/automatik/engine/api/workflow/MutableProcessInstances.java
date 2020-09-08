
package io.automatik.engine.api.workflow;

import java.util.UUID;

public interface MutableProcessInstances<T> extends ProcessInstances<T> {

	boolean exists(String id);

	void create(String id, ProcessInstance<T> instance);

	void update(String id, ProcessInstance<T> instance);

	void remove(String id);

	default boolean isActive(ProcessInstance<T> instance) {
		return instance.status() == ProcessInstance.STATE_ACTIVE || instance.status() == ProcessInstance.STATE_ERROR;
	}

	default boolean isPending(ProcessInstance<T> instance) {
		return instance.status() == ProcessInstance.STATE_PENDING;
	}

	default String resolveId(String id) {
		try {
			return UUID.fromString(id).toString();
		} catch (IllegalArgumentException e) {
			return UUID.nameUUIDFromBytes(id.getBytes()).toString();
		}
	}
}
