
package io.automatiko.engine.workflow;

import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.workflow.base.instance.AbstractProcessRuntimeServiceProvider;

public class ConfiguredProcessServices extends AbstractProcessRuntimeServiceProvider {

	public ConfiguredProcessServices(ProcessConfig config) {
		super(config.jobsService(), config.workItemHandlers(), config.processEventListeners(),
				config.signalManagerHub(), config.unitOfWorkManager(), config.variableInitializer());

	}
}
