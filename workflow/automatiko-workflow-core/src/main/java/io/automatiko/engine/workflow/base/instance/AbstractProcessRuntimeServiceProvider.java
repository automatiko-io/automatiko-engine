
package io.automatiko.engine.workflow.base.instance;

import java.util.Optional;

import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.ProcessEventListenerConfig;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.api.workflow.WorkItemHandlerConfig;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.api.workflow.signal.SignalManagerHub;
import io.automatiko.engine.services.signal.LightSignalManager;
import io.automatiko.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatiko.engine.workflow.base.instance.impl.DefaultProcessInstanceManager;

public class AbstractProcessRuntimeServiceProvider implements ProcessRuntimeServiceProvider {

	private final JobsService jobsService;
	private final ProcessInstanceManager processInstanceManager;
	private final SignalManager signalManager;
	private final WorkItemManager workItemManager;
	private final ProcessEventSupport eventSupport;
	private final UnitOfWorkManager unitOfWorkManager;
	private final VariableInitializer variableInitializer;

	public AbstractProcessRuntimeServiceProvider(JobsService jobsService, WorkItemHandlerConfig workItemHandlerProvider,
			ProcessEventListenerConfig processEventListenerProvider, SignalManagerHub compositeSignalManager,
			UnitOfWorkManager unitOfWorkManager, VariableInitializer variableInitializer) {
		this.unitOfWorkManager = unitOfWorkManager;
		this.variableInitializer = variableInitializer;
		processInstanceManager = new DefaultProcessInstanceManager();
		signalManager = new LightSignalManager(id -> Optional.ofNullable(processInstanceManager.getProcessInstance(id)),
				compositeSignalManager);
		this.eventSupport = new ProcessEventSupport(this.unitOfWorkManager);
		this.jobsService = jobsService;
		this.workItemManager = new LightWorkItemManager(null, processInstanceManager, signalManager, eventSupport);

		for (String workItem : workItemHandlerProvider.names()) {
			workItemManager.registerWorkItemHandler(workItem, workItemHandlerProvider.forName(workItem));
		}

		for (ProcessEventListener listener : processEventListenerProvider.listeners()) {
			this.eventSupport.addEventListener(listener);
		}
	}

	@Override
	public JobsService getJobsService() {
		return jobsService;
	}

	@Override
	public ProcessInstanceManager getProcessInstanceManager() {
		return processInstanceManager;
	}

	@Override
	public SignalManager getSignalManager() {
		return signalManager;
	}

	@Override
	public WorkItemManager getWorkItemManager() {
		return workItemManager;
	}

	@Override
	public ProcessEventSupport getEventSupport() {
		return eventSupport;
	}

	@Override
	public UnitOfWorkManager getUnitOfWorkManager() {
		return unitOfWorkManager;
	}

	@Override
	public VariableInitializer getVariableInitializer() {
		return variableInitializer;
	}
}
