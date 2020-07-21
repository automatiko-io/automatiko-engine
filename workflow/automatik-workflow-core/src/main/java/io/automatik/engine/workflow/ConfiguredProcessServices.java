
package io.automatik.engine.workflow;

import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.workflow.base.instance.AbstractProcessRuntimeServiceProvider;

public class ConfiguredProcessServices extends AbstractProcessRuntimeServiceProvider {

	public ConfiguredProcessServices(ProcessConfig config) {
		super(config.jobsService(), config.workItemHandlers(), config.processEventListeners(),
				config.signalManagerHub(), config.unitOfWorkManager());

	}
}
