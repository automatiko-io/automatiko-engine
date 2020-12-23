
package io.automatiko.engine.api.workflow;

import java.util.Collection;

import io.automatiko.engine.api.Model;

public interface Processes {

	Process<? extends Model> processById(String processId);

	Collection<String> processIds();

	default void activate() {

	}

	default void deactivate() {

	}
}
