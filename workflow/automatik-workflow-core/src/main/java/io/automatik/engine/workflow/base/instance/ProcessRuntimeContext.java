package io.automatik.engine.workflow.base.instance;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.workflow.VariableInitializer;
import io.automatik.engine.services.correlation.CorrelationKey;

public interface ProcessRuntimeContext {

	Collection<Process> getProcesses();

	Optional<Process> findProcess(String id);

	boolean isActive();

	ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey);

	void setupParameters(ProcessInstance pi, Map<String, Object> parameters, VariableInitializer variableInitializer);
}
