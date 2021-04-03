
package io.automatiko.engine.workflow.base.instance;

import java.util.HashMap;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;
import io.automatiko.engine.workflow.process.executable.core.ServerlessExecutableProcess;
import io.automatiko.engine.workflow.process.executable.instance.ExecutableProcessInstanceFactory;
import io.automatiko.engine.workflow.process.executable.instance.ServerlessExecutableProcessInstanceFactory;

public class ProcessInstanceFactoryRegistry {

    public static final ProcessInstanceFactoryRegistry INSTANCE = new ProcessInstanceFactoryRegistry();

    private Map<Class<? extends Process>, ProcessInstanceFactory> registry;

    private ProcessInstanceFactoryRegistry() {
        this.registry = new HashMap<Class<? extends Process>, ProcessInstanceFactory>();

        // hard wired nodes:
        register(ExecutableProcess.class, new ExecutableProcessInstanceFactory());
        register(ServerlessExecutableProcess.class, new ServerlessExecutableProcessInstanceFactory());
    }

    public void register(Class<? extends Process> cls, ProcessInstanceFactory factory) {
        this.registry.put(cls, factory);
    }

    public ProcessInstanceFactory getProcessInstanceFactory(Process process) {
        return this.registry.get(process.getClass());
    }
}
