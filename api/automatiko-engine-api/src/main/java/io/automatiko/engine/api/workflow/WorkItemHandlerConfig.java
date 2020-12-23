
package io.automatiko.engine.api.workflow;

import java.util.Collection;

import io.automatiko.engine.api.runtime.process.WorkItemHandler;

public interface WorkItemHandlerConfig {

	WorkItemHandler forName(String name);

	Collection<String> names();
}
