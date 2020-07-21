
package io.automatik.engine.workflow.base.instance;

import java.util.HashMap;
import java.util.Map;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatik.engine.workflow.process.executable.instance.ExecutableProcessInstanceFactory;

public class ProcessInstanceFactoryRegistry {

	public static final ProcessInstanceFactoryRegistry INSTANCE = new ProcessInstanceFactoryRegistry();

	private Map<Class<? extends Process>, ProcessInstanceFactory> registry;

	private ProcessInstanceFactoryRegistry() {
		this.registry = new HashMap<Class<? extends Process>, ProcessInstanceFactory>();

		// hard wired nodes:
		register(ExecutableProcess.class, new ExecutableProcessInstanceFactory());
	}

	public void register(Class<? extends Process> cls, ProcessInstanceFactory factory) {
		this.registry.put(cls, factory);
	}

	public ProcessInstanceFactory getProcessInstanceFactory(Process process) {
		return this.registry.get(process.getClass());
	}
}
