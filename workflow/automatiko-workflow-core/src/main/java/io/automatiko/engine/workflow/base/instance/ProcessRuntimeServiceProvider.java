
package io.automatiko.engine.workflow.base.instance;

import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.workflow.base.core.event.ProcessEventSupport;

public interface ProcessRuntimeServiceProvider {

    JobsService getJobsService();

    SignalManager getSignalManager();

    WorkItemManager getWorkItemManager();

    ProcessEventSupport getEventSupport();

    UnitOfWorkManager getUnitOfWorkManager();

    VariableInitializer getVariableInitializer();
}
