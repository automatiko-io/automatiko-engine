
package io.automatik.engine.workflow.process.core.node;

import io.automatik.engine.api.runtime.process.ProcessContext;
import io.automatik.engine.api.workflow.ProcessInstance;

public interface SubProcessFactory<T> {
	T bind(ProcessContext ctx);

	ProcessInstance<T> createInstance(T model);

	void unbind(ProcessContext ctx, T model);
}
