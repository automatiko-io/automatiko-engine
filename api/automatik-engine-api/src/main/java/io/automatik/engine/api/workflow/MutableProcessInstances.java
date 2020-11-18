
package io.automatik.engine.api.workflow;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MutableProcessInstances<T> extends ProcessInstances<T> {

    public static final String SEPARATOR = ":";

    boolean exists(String id);

    void create(String id, ProcessInstance<T> instance);

    void update(String id, ProcessInstance<T> instance);

    void remove(String id, ProcessInstance<T> instance);

    default boolean isActive(ProcessInstance<T> instance) {
        return instance.status() == ProcessInstance.STATE_ACTIVE || instance.status() == ProcessInstance.STATE_ERROR;
    }

    default boolean isPending(ProcessInstance<T> instance) {
        return instance.status() == ProcessInstance.STATE_PENDING;
    }

    default String resolveId(String id) {

        if (id.contains(":")) {
            String[] parts = id.split(SEPARATOR);

            return Stream.of(parts).map(this::resolveId).collect(Collectors.joining(":"));
        }

        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(id.getBytes()).toString();
        }
    }

    default String resolveId(String id, ProcessInstance<T> instance) {

        if (useCompositeIdForSubprocess() && instance.parentProcessInstanceId() != null) {
            return resolveId(instance.parentProcessInstanceId()) + SEPARATOR + resolveId(id);
        }

        return resolveId(id);
    }

    default boolean useCompositeIdForSubprocess() {
        return true;
    }

    default int calculatePage(int page, int size) {
        if (page <= 1) {
            return 0;
        }

        return (page - 1) * size;
    }
}
