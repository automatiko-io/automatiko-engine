
package io.automatik.engine.workflow.base.instance;

import io.automatik.engine.api.runtime.ProcessRuntimeFactoryService;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.workflow.ProcessConfig;

public class ProcessRuntimeFactoryServiceImpl implements ProcessRuntimeFactoryService {

	public ProcessRuntime newProcessRuntime(ProcessConfig config) {
		return new ProcessRuntimeImpl(config.processes());
	}

}
