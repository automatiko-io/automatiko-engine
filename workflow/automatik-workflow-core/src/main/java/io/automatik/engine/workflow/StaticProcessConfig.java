
package io.automatik.engine.workflow;

import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessEventListenerConfig;
import io.automatik.engine.api.workflow.ProcessInstancesFactory;
import io.automatik.engine.api.workflow.VariableInitializer;
import io.automatik.engine.api.workflow.WorkItemHandlerConfig;
import io.automatik.engine.api.workflow.signal.SignalManagerHub;
import io.automatik.engine.services.signal.DefaultSignalManagerHub;

public class StaticProcessConfig implements ProcessConfig {

    private final WorkItemHandlerConfig workItemHandlerConfig;
    private final ProcessEventListenerConfig processEventListenerConfig;
    private final SignalManagerHub signalManager;
    private final UnitOfWorkManager unitOfWorkManager;
    private final JobsService jobsService;
    private final VariableInitializer variableInitializer;

    private ProcessInstancesFactory processInstancesFactory;

    public StaticProcessConfig(WorkItemHandlerConfig workItemHandlerConfig,
            ProcessEventListenerConfig processEventListenerConfig, UnitOfWorkManager unitOfWorkManager,
            JobsService jobsService) {
        this.unitOfWorkManager = unitOfWorkManager;
        this.workItemHandlerConfig = workItemHandlerConfig;
        this.processEventListenerConfig = processEventListenerConfig;
        this.signalManager = new DefaultSignalManagerHub();
        this.jobsService = jobsService;
        this.processInstancesFactory = null;
        this.variableInitializer = null;
    }

    public StaticProcessConfig(WorkItemHandlerConfig workItemHandlerConfig,
            ProcessEventListenerConfig processEventListenerConfig, UnitOfWorkManager unitOfWorkManager, JobsService jobsService,
            ProcessInstancesFactory processInstancesFactory) {
        this.unitOfWorkManager = unitOfWorkManager;
        this.workItemHandlerConfig = workItemHandlerConfig;
        this.processEventListenerConfig = processEventListenerConfig;
        this.signalManager = new DefaultSignalManagerHub();
        this.jobsService = jobsService;
        this.processInstancesFactory = processInstancesFactory;
        this.variableInitializer = null;
    }

    public StaticProcessConfig(WorkItemHandlerConfig workItemHandlerConfig,
            ProcessEventListenerConfig processEventListenerConfig, UnitOfWorkManager unitOfWorkManager, JobsService jobsService,
            VariableInitializer variableInitializer) {
        this.unitOfWorkManager = unitOfWorkManager;
        this.workItemHandlerConfig = workItemHandlerConfig;
        this.processEventListenerConfig = processEventListenerConfig;
        this.signalManager = new DefaultSignalManagerHub();
        this.jobsService = jobsService;
        this.variableInitializer = variableInitializer;
    }

    @Override
    public WorkItemHandlerConfig workItemHandlers() {
        return this.workItemHandlerConfig;
    }

    @Override
    public ProcessEventListenerConfig processEventListeners() {
        return this.processEventListenerConfig;
    }

    @Override
    public SignalManagerHub signalManagerHub() {
        return this.signalManager;
    }

    @Override
    public UnitOfWorkManager unitOfWorkManager() {
        return this.unitOfWorkManager;
    }

    @Override
    public JobsService jobsService() {
        return jobsService;
    }

    @Override
    public ProcessInstancesFactory processInstancesFactory() {
        return processInstancesFactory;
    }

    @Override
    public VariableInitializer variableInitializer() {
        return variableInitializer;
    }
}
