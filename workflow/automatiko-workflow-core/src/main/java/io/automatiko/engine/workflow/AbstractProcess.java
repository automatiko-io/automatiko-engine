
package io.automatiko.engine.workflow;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.AccessPolicy;
import io.automatiko.engine.api.jobs.DurationExpirationTime;
import io.automatiko.engine.api.jobs.ExactExpirationTime;
import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.workflow.ArchiveBuilder;
import io.automatiko.engine.api.workflow.ArchivedProcessInstance;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.ExportedProcessInstance;
import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;
import io.automatiko.engine.api.workflow.Signal;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.automatiko.engine.services.signal.EventListenerResolver;
import io.automatiko.engine.services.signal.LightSignalManager;
import io.automatiko.engine.workflow.auth.AccessPolicyFactory;
import io.automatiko.engine.workflow.auth.AllowAllAccessPolicy;
import io.automatiko.engine.workflow.base.core.timer.CronExpirationTime;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.LightProcessRuntime;
import io.automatiko.engine.workflow.base.instance.LightProcessRuntimeContext;
import io.automatiko.engine.workflow.base.instance.LightProcessRuntimeServiceProvider;
import io.automatiko.engine.workflow.base.instance.ProcessRuntimeServiceProvider;
import io.automatiko.engine.workflow.base.instance.impl.end.RemoveEndOfInstanceStrategy;
import io.automatiko.engine.workflow.lock.LockManager;
import io.automatiko.engine.workflow.process.core.impl.WorkflowProcessImpl;
import io.automatiko.engine.workflow.process.core.node.StartNode;

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

    protected io.automatiko.engine.api.definition.process.Process process;

    protected LockManager locks = new LockManager();

    protected EndOfInstanceStrategy endOfInstanceStrategy = new RemoveEndOfInstanceStrategy();

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
    public String description() {
        return (String) process().getMetaData().get("Documentation");
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

    public abstract ProcessInstance<T> createInstance(WorkflowProcessInstance wpi, T m, long versionracker);

    public abstract ProcessInstance<T> createReadOnlyInstance(WorkflowProcessInstance wpi, T m);

    @Override
    public ProcessInstances<T> instances() {
        UnitOfWork unitOfWork = services.getUnitOfWorkManager().currentUnitOfWork();
        return (ProcessInstances<T>) unitOfWork.managedProcessInstances(this, instances);

    }

    @Override
    public <S> void send(Signal<S> signal) {
        instances().values(ProcessInstanceReadMode.MUTABLE, 1, 10).forEach(pi -> pi.send(signal));
    }

    public Process<T> configure() {
        if (this.services.getSignalManager() instanceof LightSignalManager) {
            ((LightSignalManager) this.services.getSignalManager()).setInstanceResolver(new ProcessEventListenerResolver());
        }
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
                if (CronExpirationTime.isCronExpression(timer.getDelay())) {
                    return CronExpirationTime.of(timer.getDelay());
                } else {

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
                }

            case Timer.TIME_DURATION:
                long duration = DateTimeUtils.parseDuration(timer.getDelay());
                return DurationExpirationTime.repeat(duration);

            case Timer.TIME_DATE:

                try {
                    return ExactExpirationTime.of(timer.getDate());
                } catch (DateTimeParseException e) {
                    throw new WorkItemExecutionError("Parsing of date and time for timer failed",
                            "DateTimeParseFailure",
                            "Unable to parse '" + timer.getDate() + "' as valid ISO date and time format", e);
                }

            default:
                throw new UnsupportedOperationException("Not supported timer definition");
        }
    }

    public io.automatiko.engine.api.definition.process.Process process() {
        if (this.process == null) {
            this.process = buildProcess();
        }
        return this.process;
    }

    public abstract io.automatiko.engine.api.definition.process.Process buildProcess();

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

    public LockManager locks() {
        return locks;
    }

    public ProcessRuntimeServiceProvider services() {
        return services;
    }

    public EndOfInstanceStrategy endOfInstanceStrategy() {
        return endOfInstanceStrategy;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ExportedProcessInstance exportInstance(String id, boolean abort) {
        return ((MutableProcessInstances<?>) instances()).exportInstance(id, abort);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ProcessInstance<T> importInstance(ExportedProcessInstance instance) {
        ProcessInstance<T> importedInstance = ((MutableProcessInstances<?>) instances()).importInstance(instance,
                (Process) this);
        ((AbstractProcessInstance<T>) importedInstance).imported();
        return importedInstance;
    }

    public ArchivedProcessInstance archiveInstance(String id, ArchiveBuilder builder) {
        ExportedProcessInstance<?> exported = exportInstance(id, false);

        ArchivedProcessInstance archived = builder.instance(id, this.id(), exported);

        ProcessInstance<?> instance = instances().findById(id, ProcessInstanceReadMode.READ_ONLY).get();

        Map<String, Object> variables = ((AbstractProcessInstance<?>) instance).processInstance().getVariables();

        for (Entry<String, Object> entry : variables.entrySet()) {

            archived.addVariable(builder.variable(entry.getKey(), entry.getValue()));
        }

        return archived;
    }

    protected class CompletionEventListener implements EventListener {

        public CompletionEventListener() {
            // Do nothing
        }

        @Override
        public void signalEvent(String type, Object event) {

            if (type.startsWith("processInstanceCompleted:")) {
                io.automatiko.engine.api.runtime.process.ProcessInstance pi = (io.automatiko.engine.api.runtime.process.ProcessInstance) event;

                if (!id().equals(pi.getProcessId()) && pi.getParentProcessInstanceId() != null) {
                    instances().findById(pi.getParentProcessInstanceId(), ProcessInstanceReadMode.MUTABLE_WITH_LOCK)
                            .ifPresent(p -> {
                                p.send(Sig.of(type, event));
                            });
                }
            }
        }

        @Override
        public String[] getEventTypes() {
            return new String[0];
        }
    }

    private class ProcessEventListenerResolver implements EventListenerResolver {

        @Override
        public Optional<EventListener> find(String id) {
            Optional.ofNullable(instances().findById(id)
                    .map(pi -> ((AbstractProcessInstance<?>) pi).internalGetProcessInstance()).orElse(null));
            return null;
        }

    }
}
