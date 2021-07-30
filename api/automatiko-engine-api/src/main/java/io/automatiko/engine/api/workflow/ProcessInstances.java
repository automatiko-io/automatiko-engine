
package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

public interface ProcessInstances<T> {

    default Optional<? extends ProcessInstance<T>> findById(String id) {
        return findById(id, ProcessInstanceReadMode.MUTABLE);
    }

    default Optional<? extends ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode) {
        return findById(id, ProcessInstance.STATE_ACTIVE, mode);
    }

    Optional<? extends ProcessInstance<T>> findById(String id, int status, ProcessInstanceReadMode mode);

    default Collection<? extends ProcessInstance<T>> values(int page, int size) {
        return values(ProcessInstanceReadMode.READ_ONLY, page, size);
    }

    default Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int page, int size) {
        return values(mode, ProcessInstance.STATE_ACTIVE, page, size);
    }

    Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int status, int page, int size);

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(String... values) {
        return findByIdOrTag(ProcessInstanceReadMode.MUTABLE, values);
    }

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, String... values) {
        return findByIdOrTag(mode, ProcessInstance.STATE_ACTIVE, values);
    }

    Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, int status, String... values);

    Long size();

}
