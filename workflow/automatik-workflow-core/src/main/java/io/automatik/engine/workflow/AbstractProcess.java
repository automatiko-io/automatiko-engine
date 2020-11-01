
package io.automatik.engine.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.AccessPolicy;
import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.ExactExpirationTime;
import io.automatik.engine.api.jobs.ExpirationTime;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.api.uow.UnitOfWork;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.api.workflow.ProcessInstances;
import io.automatik.engine.api.workflow.ProcessInstancesFactory;
import io.automatik.engine.api.workflow.Signal;
import io.automatik.engine.workflow.auth.AccessPolicyFactory;
import io.automatik.engine.workflow.auth.AllowAllAccessPolicy;
import io.automatik.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.base.instance.LightProcessRuntime;
import io.automatik.engine.workflow.base.instance.LightProcessRuntimeContext;
import io.automatik.engine.workflow.base.instance.LightProcessRuntimeServiceProvider;
import io.automatik.engine.workflow.base.instance.ProcessRuntimeServiceProvider;
import io.automatik.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatik.engine.workflow.process.core.node.StartNode;

@SuppressWarnings("unchecked")
public abstract class AbstractProcess<T extends Model> implements Process<T> {

    protected final ProcessRuntimeServiceProvider services;
    protected ProcessInstancesFactory processInstancesFactory;
    protected MutableProcessInstances<T> instances;
    protected CompletionEventListener completionEventListener = new CompletionEventListener();

    protected boolean activated;
    protected List<String> startTimerInstances = new ArrayList<>();
    protected ProcessRuntime processRuntime;

    protected AccessPolicy<ProcessInstance<T>> accessPolicy = new AllowAllAccessPolicy<T>();

    protected AbstractProcess() {
        this(new LightProcessRuntimeServiceProvider());
    }

    protected AbstractProcess(MutableProcessInstances<T> instances) {
        this(new LightProcessRuntimeServiceProvider(), instances);
    }

    protected AbstractProcess(ProcessConfig config) {
        this(new ConfiguredProcessServices(config));
        if (config.processInstancesFactory() != null) {
            this.processInstancesFactory = config.processInstancesFactory();
        }

    }

    protected AbstractProcess(ProcessConfig config, MutableProcessInstances<T> instances) {
        this(new ConfiguredProcessServices(config), instances);
        if (config.processInstancesFactory() != null) {
            this.processInstancesFactory = config.processInstancesFactory();
        }
    }

    protected AbstractProcess(ProcessRuntimeServiceProvider services) {
        this(services, new MapProcessInstances());

    }

    protected AbstractProcess(ProcessRuntimeServiceProvider services, MutableProcessInstances<T> instances) {
        this.services = services;
        this.instances = instances;
    }

    @Override
    public String id() {
        if (version() != null) {
            return process().getId() + "_" + version().replaceAll("\\.", "_");
        } else {
            return process().getId();
        }
    }

    @Override
    public String name() {
        return process().getName();
    }

    @Override
    public String version() {
        return process().getVersion();
    }

    @Override
    public T createModel() {
        return null;
    }

    @Override
    public ProcessInstance<T> createInstance(String businessKey, Model m) {
        return createInstance(businessKey, m);
    }

    public abstract ProcessInstance<T> createInstance(WorkflowProcessInstance wpi, T m);

    public abstract ProcessInstance<T> createReadOnlyInstance(WorkflowProcessInstance wpi, T m);

    @Override
    public ProcessInstances<T> instances() {
        UnitOfWork unitOfWork = services.getUnitOfWorkManager().currentUnitOfWork();
        return (ProcessInstances<T>) unitOfWork.managedProcessInstances(this, instances);

    }

    @Override
    public <S> void send(Signal<S> signal) {
        instances().values(ProcessInstanceReadMode.MUTABLE).forEach(pi -> pi.send(signal));
    }

    public Process<T> configure() {
        this.accessPolicy = AccessPolicyFactory.newPolicy((String) process().getMetaData().get("accessPolicy"));
        registerListeners();
        if (isProcessFactorySet()) {
            this.instances = (MutableProcessInstances<T>) processInstancesFactory.createProcessInstances(this);
        }

        return this;
    }

    protected void registerListeners() {

    }

    @Override
    public void activate() {
        if (this.activated) {
            return;
        }

        configure();
        WorkflowProcessImpl p = (WorkflowProcessImpl) process();
        List<StartNode> startNodes = p.getTimerStart();
        if (startNodes != null && !startNodes.isEmpty()) {
            this.processRuntime = createProcessRuntime();
            for (StartNode startNode : startNodes) {
                if (startNode != null && startNode.getTimer() != null) {
                    String timerId = processRuntime.getJobsService().scheduleProcessJob(
                            ProcessJobDescription.of(configureTimerInstance(startNode.getTimer()), this));
                    startTimerInstances.add(timerId);
                }
            }
        }
        this.activated = true;
    }

    @Override
    public void deactivate() {
        for (String startTimerId : startTimerInstances) {
            this.processRuntime.getJobsService().cancelJob(startTimerId);
        }
        this.activated = false;
    }

    @Override
    public AccessPolicy<ProcessInstance<T>> accessPolicy() {
        return accessPolicy;
    }

    protected ExpirationTime configureTimerInstance(Timer timer) {
        switch (timer.getTimeType()) {
            case Timer.TIME_CYCLE:
                // when using ISO date/time period is not set
                long[] repeatValues = DateTimeUtils.parseRepeatableDateTime(timer.getDelay());
                if (repeatValues.length == 3) {
                    int parsedReapedCount = (int) repeatValues[0];
                    if (parsedReapedCount <= -1) {
                        parsedReapedCount = Integer.MAX_VALUE;
                    }
                    return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2], parsedReapedCount);
                } else if (repeatValues.length == 2) {
                    return DurationExpirationTime.repeat(repeatValues[0], repeatValues[1], Integer.MAX_VALUE);
                } else {
                    return DurationExpirationTime.repeat(repeatValues[0], repeatValues[0], Integer.MAX_VALUE);
                }

            case Timer.TIME_DURATION:
                long duration = DateTimeUtils.parseDuration(timer.getDelay());
                return DurationExpirationTime.repeat(duration);

            case Timer.TIME_DATE:

                return ExactExpirationTime.of(timer.getDate());

            default:
                throw new UnsupportedOperationException("Not supported timer definition");
        }
    }

    public abstract io.automatik.engine.api.definition.process.Process process();

    protected ProcessRuntime createProcessRuntime() {
        return new LightProcessRuntime(new LightProcessRuntimeContext(Collections.singletonList(process())), services);
    }

    protected boolean isProcessFactorySet() {
        return processInstancesFactory != null;
    }

    public void setProcessInstancesFactory(ProcessInstancesFactory processInstancesFactory) {
        this.processInstancesFactory = processInstancesFactory;
    }

    public EventListener eventListener() {
        return completionEventListener;
    }

    protected class CompletionEventListener implements EventListener {

        public CompletionEventListener() {
            // Do nothing
        }

        @Override
        public void signalEvent(String type, Object event) {
            if (type.startsWith("processInstanceCompleted:")) {
                io.automatik.engine.api.runtime.process.ProcessInstance pi = (io.automatik.engine.api.runtime.process.ProcessInstance) event;
                if (!id().equals(pi.getProcessId()) && pi.getParentProcessInstanceId() != null) {
                    instances().findById(pi.getParentProcessInstanceId()).ifPresent(p -> p.send(Sig.of(type, event)));
                }
            }
        }

        @Override
        public String[] getEventTypes() {
            return new String[0];
        }
    }
}
