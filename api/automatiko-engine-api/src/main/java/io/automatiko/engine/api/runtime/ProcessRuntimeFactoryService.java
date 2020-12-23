package io.automatiko.engine.api.runtime;

import io.automatiko.engine.api.Service;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.workflow.ProcessConfig;

public interface ProcessRuntimeFactoryService extends Service {

	ProcessRuntime newProcessRuntime(ProcessConfig config);

}
