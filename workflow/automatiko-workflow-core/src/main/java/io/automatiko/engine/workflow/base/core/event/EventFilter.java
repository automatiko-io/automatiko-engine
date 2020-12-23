
package io.automatiko.engine.workflow.base.core.event;

import java.util.function.Function;

public interface EventFilter {

	boolean acceptsEvent(String type, Object event);

	boolean acceptsEvent(String type, Object event, Function<String, String> resolver);

}
