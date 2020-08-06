
package io.automatik.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

public interface ProcessInstances<T> {

	default Optional<ProcessInstance<T>> findById(String id) {
		return findById(id, ProcessInstanceReadMode.MUTABLE);
	}

	Optional<ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode);

	default Collection<ProcessInstance<T>> values() {
		return values(ProcessInstanceReadMode.READ_ONLY);
	}

	Collection<ProcessInstance<T>> values(ProcessInstanceReadMode mode);

	Integer size();

}
