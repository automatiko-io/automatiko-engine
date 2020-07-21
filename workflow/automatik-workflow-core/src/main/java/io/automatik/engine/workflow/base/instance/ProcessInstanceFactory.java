
package io.automatik.engine.workflow.base.instance;

import java.util.Map;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.services.correlation.CorrelationKey;

/**
 * 
 */
public interface ProcessInstanceFactory {

	ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey,
			InternalProcessRuntime runtime, Map<String, Object> parameters);

}
