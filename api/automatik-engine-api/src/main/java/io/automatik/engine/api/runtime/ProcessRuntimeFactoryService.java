package io.automatik.engine.api.runtime;

import io.automatik.engine.api.Service;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.workflow.ProcessConfig;

public interface ProcessRuntimeFactoryService extends Service {

	ProcessRuntime newProcessRuntime(ProcessConfig config);

}
