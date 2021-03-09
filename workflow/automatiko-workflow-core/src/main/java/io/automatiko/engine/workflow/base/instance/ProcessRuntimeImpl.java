
package io.automatiko.engine.workflow.base.instance;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.jobs.DurationExpirationTime;
import io.automatiko.engine.api.jobs.ExactExpirationTime;
import io.automatiko.engine.api.jobs.ExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.api.runtime.process.EventListener;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.VariableInitializer;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.services.correlation.CorrelationKey;
import io.automatiko.engine.services.jobs.impl.InMemoryJobService;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTransformer;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatiko.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatiko.engine.workflow.base.core.timer.TimeUtils;
import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatiko.engine.workflow.base.instance.event.DefaultSignalManagerFactory;
import io.automatiko.engine.workflow.base.instance.impl.DefaultProcessInstanceManagerFactory;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.Trigger;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class ProcessRuntimeImpl implements InternalProcessRuntime {

    private Map<String, Process> processes;
    private ProcessInstanceManager processInstanceManager;
    private SignalManager signalManager;
    private JobsService jobService;
    private ProcessEventSupport processEventSupport;
    private UnitOfWorkManager unitOfWorkManager;
    private WorkItemManager workItemManager;
    private VariableInitializer variableInitializer = new DefaultVariableInitializer();

    public ProcessRuntimeImpl(Map<String, Process> processes) {
        this.processes = processes;
        initProcessInstanceManager();
        initSignalManager();
        unitOfWorkManager = new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory());
        jobService = new InMemoryJobService(this, unitOfWorkManager);
        processEventSupport = new ProcessEventSupport(unitOfWorkManager);
        workItemManager = new DefaultWorkItemManager(this);
        initProcessEventListeners();
        initStartTimers();

    }

    public void initStartTimers() {
        Collection<Process> processes = this.processes.values();
        for (Process process : processes) {
            ExecutableProcess p = (ExecutableProcess) process;
            List<StartNode> startNodes = p.getTimerStart();
            if (startNodes != null && !startNodes.isEmpty()) {

                for (StartNode startNode : startNodes) {
                    if (startNode != null && startNode.getTimer() != null) {
                        jobService.scheduleProcessJob(ProcessJobDescription
                                .of(createTimerInstance(startNode.getTimer()), p.getId(), p.getVersion()));
                    }
                }
            }
        }
    }

    private void initProcessInstanceManager() {
        processInstanceManager = new DefaultProcessInstanceManagerFactory().createProcessInstanceManager(this);
    }

    private void initSignalManager() {
        signalManager = new DefaultSignalManagerFactory().createSignalManager(this);
    }

    public ProcessInstance startProcess(final String processId) {
        return startProcess(processId, null);
    }

    public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
        return startProcess(processId, parameters, null, null);
    }

    public ProcessInstance startProcess(String processId, Map<String, Object> parameters, String trigger,
            Object triggerData) {
        ProcessInstance processInstance = createProcessInstance(processId, parameters);
        if (processInstance != null) {
            // start process instance
            return startProcessInstance(processInstance.getId(), trigger, triggerData);
        }
        return null;
    }

    public ProcessInstance createProcessInstance(String processId, Map<String, Object> parameters) {
        return createProcessInstance(processId, null, parameters);
    }

    public ProcessInstance startProcessInstance(String processInstanceId, String trigger, Object triggerData) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        ((io.automatiko.engine.workflow.base.instance.ProcessInstance) processInstance).configureSLA();
        getProcessEventSupport().fireBeforeProcessStarted(processInstance, this);
        ((io.automatiko.engine.workflow.base.instance.ProcessInstance) processInstance).start(trigger, triggerData);
        getProcessEventSupport().fireAfterProcessStarted(processInstance, this);
        return processInstance;
    }

    public ProcessInstance startProcessInstance(String processInstanceId) {
        return startProcessInstance(processInstanceId, null, null);
    }

    @Override
    public ProcessInstance startProcess(String processId, CorrelationKey correlationKey,
            Map<String, Object> parameters) {
        ProcessInstance processInstance = createProcessInstance(processId, correlationKey, parameters);
        if (processInstance != null) {
            return startProcessInstance(processInstance.getId());
        }
        return null;
    }

    @Override
    public ProcessInstance createProcessInstance(String processId, CorrelationKey correlationKey,
            Map<String, Object> parameters) {
        final Process process = processes.get(processId);
        if (process == null) {
            throw new IllegalArgumentException("Unknown process ID: " + processId);
        }
        return startProcess(process, correlationKey, parameters);
    }

    @Override
    public ProcessInstance getProcessInstance(CorrelationKey correlationKey) {
        return processInstanceManager.getProcessInstance(correlationKey);
    }

    private io.automatiko.engine.workflow.base.instance.ProcessInstance startProcess(Process process,
            CorrelationKey correlationKey, Map<String, Object> parameters) {
        ProcessInstanceFactory conf = ProcessInstanceFactoryRegistry.INSTANCE.getProcessInstanceFactory(process);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal process type: " + process.getClass());
        }
        return conf.createProcessInstance(process, correlationKey, this, parameters, variableInitializer);
    }

    public ProcessInstanceManager getProcessInstanceManager() {
        return processInstanceManager;
    }

    public JobsService getJobsService() {
        return jobService;
    }

    public SignalManager getSignalManager() {
        return signalManager;
    }

    public Collection<ProcessInstance> getProcessInstances() {
        return processInstanceManager.getProcessInstances();
    }

    public ProcessInstance getProcessInstance(String id) {
        return getProcessInstance(id, false);
    }

    public ProcessInstance getProcessInstance(String id, boolean readOnly) {
        return processInstanceManager.getProcessInstance(id, readOnly);
    }

    public void removeProcessInstance(ProcessInstance processInstance) {
        processInstanceManager.removeProcessInstance(processInstance);
    }

    public void initProcessEventListeners() {
        for (Process process : this.processes.values()) {
            initProcessEventListener(process);
        }
    }

    public void removeProcessEventListeners() {
        for (Process process : this.processes.values()) {
            removeProcessEventListener(process);
        }
    }

    private void removeProcessEventListener(Process process) {
        if (process instanceof ExecutableProcess) {
            String type = (String) ((ExecutableProcess) process).getRuntimeMetaData().get("StartProcessEventType");
            StartProcessEventListener listener = (StartProcessEventListener) ((ExecutableProcess) process)
                    .getRuntimeMetaData().get("StartProcessEventListener");
            if (type != null && listener != null) {
                signalManager.removeEventListener(type, listener);
            }
        }
    }

    private void initProcessEventListener(Process process) {
        if (process instanceof ExecutableProcess) {
            for (Node node : ((ExecutableProcess) process).getNodes()) {
                if (node instanceof StartNode) {
                    StartNode startNode = (StartNode) node;
                    if (startNode != null) {
                        List<Trigger> triggers = startNode.getTriggers();
                        if (triggers != null) {
                            for (Trigger trigger : triggers) {
                                if (trigger instanceof EventTrigger) {
                                    final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();
                                    String type = null;
                                    for (EventFilter filter : filters) {
                                        if (filter instanceof EventTypeFilter) {
                                            type = ((EventTypeFilter) filter).getType();
                                        }
                                    }
                                    StartProcessEventListener listener = new StartProcessEventListener(process.getId(),
                                            filters, trigger.getInMappings(), startNode.getEventTransformer());
                                    signalManager.addEventListener(type, listener);
                                    ((ExecutableProcess) process).getRuntimeMetaData().put("StartProcessEventType",
                                            type);
                                    ((ExecutableProcess) process).getRuntimeMetaData().put("StartProcessEventListener",
                                            listener);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public ProcessEventSupport getProcessEventSupport() {
        return processEventSupport;
    }

    public void setProcessEventSupport(ProcessEventSupport processEventSupport) {
        this.processEventSupport = processEventSupport;
    }

    public void addEventListener(final ProcessEventListener listener) {
        this.processEventSupport.addEventListener(listener);
    }

    public void removeEventListener(final ProcessEventListener listener) {
        this.processEventSupport.removeEventListener(listener);
    }

    public List<ProcessEventListener> getProcessEventListeners() {
        return processEventSupport.getEventListeners();
    }

    private void startProcessWithParamsAndTrigger(String processId, Map<String, Object> params, String type,
            boolean dispose, Object triggerData) {

        startProcess(processId, params, type, triggerData);
    }

    public void abortProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (processInstance == null) {
            throw new IllegalArgumentException("Could not find process instance for id " + processInstanceId);
        }
        ((io.automatiko.engine.workflow.base.instance.ProcessInstance) processInstance)
                .setState(ProcessInstance.STATE_ABORTED);
    }

    public WorkItemManager getWorkItemManager() {
        return workItemManager;
    }

    @Override
    public UnitOfWorkManager getUnitOfWorkManager() {
        return unitOfWorkManager;
    }

    public void signalEvent(String type, Object event) {
        signalManager.signalEvent(type, event);
    }

    public void signalEvent(String type, Object event, String processInstanceId) {
        signalManager.signalEvent(processInstanceId, type, event);
    }

    public void dispose() {
        this.processEventSupport.reset();
    }

    public void clearProcessInstances() {
        this.processInstanceManager.clearProcessInstances();
    }

    public void clearProcessInstancesState() {
        this.processInstanceManager.clearProcessInstancesState();
    }

    protected ExpirationTime createTimerInstance(Timer timer) {

        return configureTimerInstance(timer);

    }

    private ExpirationTime configureTimerInstance(Timer timer) {
        long duration = -1;
        switch (timer.getTimeType()) {
            case Timer.TIME_CYCLE:
                // when using ISO date/time period is not set
                long[] repeatValues = DateTimeUtils.parseRepeatableDateTime(timer.getDelay());
                if (repeatValues.length == 3) {
                    int parsedReapedCount = (int) repeatValues[0];

                    return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2], parsedReapedCount);
                } else {
                    long delay = repeatValues[0];
                    long period = -1;
                    try {
                        period = TimeUtils.parseTimeString(timer.getPeriod());
                    } catch (RuntimeException e) {
                        period = repeatValues[0];
                    }

                    return DurationExpirationTime.repeat(delay, period);
                }

            case Timer.TIME_DURATION:

                duration = DateTimeUtils.parseDuration(timer.getDelay());
                return DurationExpirationTime.after(duration);

            case Timer.TIME_DATE:

                return ExactExpirationTime.of(timer.getDate());

            default:
                throw new UnsupportedOperationException("Not supported timer definition");
        }
    }

    private class StartProcessEventListener implements EventListener {

        private String processId;
        private List<EventFilter> eventFilters;
        private Map<String, String> inMappings;
        private EventTransformer eventTransformer;

        public StartProcessEventListener(String processId, List<EventFilter> eventFilters,
                Map<String, String> inMappings, EventTransformer eventTransformer) {
            this.processId = processId;
            this.eventFilters = eventFilters;
            this.inMappings = inMappings;
            this.eventTransformer = eventTransformer;
        }

        public String[] getEventTypes() {
            return null;
        }

        public void signalEvent(final String type, Object event) {
            for (EventFilter filter : eventFilters) {
                if (!filter.acceptsEvent(type, event)) {
                    return;
                }
            }
            if (eventTransformer != null) {
                event = eventTransformer.transformEvent(event);
            }
            Map<String, Object> params = null;
            if (inMappings != null && !inMappings.isEmpty()) {
                params = new HashMap<String, Object>();

                if (inMappings.size() == 1) {
                    params.put(inMappings.keySet().iterator().next(), event);
                } else {
                    for (Map.Entry<String, String> entry : inMappings.entrySet()) {
                        if ("event".equals(entry.getValue())) {
                            params.put(entry.getKey(), event);
                        } else {
                            params.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
            startProcessWithParamsAndTrigger(processId, params, type, false, event);
        }
    }

    public Process getProcess(String id) {
        return this.processes.get(id);
    }

    @Override
    public VariableInitializer getVariableInitializer() {
        return variableInitializer;
    }
}
