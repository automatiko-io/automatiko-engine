
package io.automatiko.engine.api.workflow;

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
        if (instance.status() != ProcessInstance.STATE_PENDING
                && instance.process().endOfInstanceStrategy().shouldInstanceBeUpdated()) {
            return true;
        }

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

    default ExportedProcessInstance exportInstance(String id) {
        return exportInstance(findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id)), false);
    }

    default ExportedProcessInstance exportInstance(String id, boolean abort) {
        return exportInstance(findById(id).orElseThrow(() -> new ProcessInstanceNotFoundException(id)), abort);
    }

    ExportedProcessInstance exportInstance(ProcessInstance<?> instance, boolean abort);

    ProcessInstance<T> importInstance(ExportedProcessInstance instance, Process<T> process);

    default void release(String id, ProcessInstance<T> pi) {

    }
}
