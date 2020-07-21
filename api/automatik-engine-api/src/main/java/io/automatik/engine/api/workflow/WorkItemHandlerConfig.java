
package io.automatik.engine.api.workflow;

import java.util.Collection;

import io.automatik.engine.api.runtime.process.WorkItemHandler;

public interface WorkItemHandlerConfig {

	WorkItemHandler forName(String name);

	Collection<String> names();
}
