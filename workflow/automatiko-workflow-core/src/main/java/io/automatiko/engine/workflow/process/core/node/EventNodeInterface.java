
package io.automatiko.engine.workflow.process.core.node;

import java.util.function.Function;

public interface EventNodeInterface {

	boolean acceptsEvent(String type, Object event);

	default boolean acceptsEvent(String type, Object event, Function<String, String> resolver) {
		return acceptsEvent(type, event);
	}

	String getVariableName();

}
