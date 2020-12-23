
package io.automatiko.engine.workflow.base.instance;

import java.util.Map;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.services.correlation.CorrelationKey;

/**
 * 
 */
public interface ProcessInstanceFactory {

	ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey,
			InternalProcessRuntime runtime, Map<String, Object> parameters, VariableInitializer variableInitializer);

}
