
package io.automatik.engine.workflow.base.instance;

import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.VariableInitializer;
import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.workflow.base.core.event.ProcessEventSupport;

public interface ProcessRuntimeServiceProvider {

	JobsService getJobsService();

	ProcessInstanceManager getProcessInstanceManager();

	SignalManager getSignalManager();

	WorkItemManager getWorkItemManager();

	ProcessEventSupport getEventSupport();

	UnitOfWorkManager getUnitOfWorkManager();

	VariableInitializer getVariableInitializer();
}
