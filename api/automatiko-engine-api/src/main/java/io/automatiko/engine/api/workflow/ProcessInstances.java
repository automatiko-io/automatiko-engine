
package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

public interface ProcessInstances<T> {

    default Optional<? extends ProcessInstance<T>> findById(String id) {
        return findById(id, ProcessInstanceReadMode.MUTABLE);
    }

    Optional<? extends ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode);

    default Collection<? extends ProcessInstance<T>> values(int page, int size) {
        return values(ProcessInstanceReadMode.READ_ONLY, page, size);
    }

    Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode, int page, int size);

    default Collection<? extends ProcessInstance<T>> findByIdOrTag(String... values) {
        return findByIdOrTag(ProcessInstanceReadMode.MUTABLE, values);
    }

    Collection<? extends ProcessInstance<T>> findByIdOrTag(ProcessInstanceReadMode mode, String... values);

    Integer size();

}
