
package io.automatik.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

public interface ProcessInstances<T> {

    default Optional<? extends ProcessInstance<T>> findById(String id) {
        return findById(id, ProcessInstanceReadMode.MUTABLE);
    }

    Optional<? extends ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode);

    default Collection<? extends ProcessInstance<T>> values() {
        return values(ProcessInstanceReadMode.READ_ONLY);
    }

    Collection<? extends ProcessInstance<T>> values(ProcessInstanceReadMode mode);

    Integer size();

}
