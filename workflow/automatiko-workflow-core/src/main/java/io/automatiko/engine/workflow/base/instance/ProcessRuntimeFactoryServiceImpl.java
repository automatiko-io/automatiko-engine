
package io.automatiko.engine.workflow.base.instance;

import io.automatiko.engine.api.runtime.ProcessRuntimeFactoryService;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.workflow.ProcessConfig;

public class ProcessRuntimeFactoryServiceImpl implements ProcessRuntimeFactoryService {

	public ProcessRuntime newProcessRuntime(ProcessConfig config) {
		return new ProcessRuntimeImpl(config.processes());
	}

}
