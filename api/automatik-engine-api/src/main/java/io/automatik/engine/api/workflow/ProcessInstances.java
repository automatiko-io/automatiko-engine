
package io.automatik.engine.api.workflow;

import java.util.Collection;
import java.util.Optional;

public interface ProcessInstances<T> {

	Optional<? extends ProcessInstance<T>> findById(String id);

	Collection<? extends ProcessInstance<T>> values();

}
