
package io.automatik.engine.workflow.base.instance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.event.process.ProcessEventListener;
import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.ExactExpirationTime;
import io.automatik.engine.api.jobs.ExpirationTime;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.WorkItemManager;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.workflow.signal.SignalManager;
import io.automatik.engine.services.correlation.CorrelationKey;
import io.automatik.engine.services.jobs.impl.InMemoryJobService;
import io.automatik.engine.workflow.base.core.event.EventFilter;
import io.automatik.engine.workflow.base.core.event.EventTransformer;
import io.automatik.engine.workflow.base.core.event.EventTypeFilter;
import io.automatik.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatik.engine.workflow.base.core.timer.DateTimeUtils;
import io.automatik.engine.workflow.base.core.timer.TimeUtils;
import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.process.core.node.EventTrigger;
import io.automatik.engine.workflow.process.core.node.StartNode;
import io.automatik.engine.workflow.process.core.node.Trigger;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;

public class LightProcessRuntime implements InternalProcessRuntime {

	private ProcessRuntimeContext runtimeContext;

	private ProcessInstanceManager processInstanceManager;
	private SignalManager signalManager;
	private JobsService jobService;
	private ProcessEventSupport processEventSupport;
	private final WorkItemManager workItemManager;
	private UnitOfWorkManager unitOfWorkManager;

	public static LightProcessRuntime ofProcess(Process p) {
		LightProcessRuntimeServiceProvider services = new LightProcessRuntimeServiceProvider();

		LightProcessRuntimeContext rtc = new LightProcessRuntimeContext(Collections.singletonList(p));

		return new LightProcessRuntime(rtc, services);
	}

	public LightProcessRuntime(ProcessRuntimeContext runtimeContext, ProcessRuntimeServiceProvider services) {
		this.unitOfWorkManager = services.getUnitOfWorkManager();
		this.runtimeContext = runtimeContext;
		this.processInstanceManager = services.getProcessInstanceManager();
		this.signalManager = services.getSignalManager();
		this.jobService = services.getJobsService() == null ? new InMemoryJobService(this, this.unitOfWorkManager)
				: services.getJobsService();
		this.processEventSupport = services.getEventSupport();
		this.workItemManager = services.getWorkItemManager();

		if (isActive()) {
			initProcessEventListeners();
			initStartTimers();
		}
	}

	public void initStartTimers() {
		Collection<Process> processes = runtimeContext.getProcesses();
		for (Process process : processes) {
			ExecutableProcess p = (ExecutableProcess) process;
			List<StartNode> startNodes = p.getTimerStart();
			if (startNodes != null && !startNodes.isEmpty()) {
				for (StartNode startNode : startNodes) {
					if (startNode != null && startNode.getTimer() != null) {

						jobService.scheduleProcessJob(
								ProcessJobDescription.of(createTimerInstance(startNode.getTimer()), p.getId()));

					}
				}
			}
		}
	}

	public ProcessInstance startProcess(String processId) {
		return startProcess(processId, null);
	}

	public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
		return startProcess(processId, parameters, null);
	}

	public ProcessInstance startProcess(String processId, Map<String, Object> parameters, String trigger) {
		ProcessInstance processInstance = createProcessInstance(processId, parameters);
		if (processInstance != null) {
			processInstanceManager.addProcessInstance(processInstance, null);
			return startProcessInstance(processInstance.getId(), trigger);
		}
		return null;
	}

	public ProcessInstance createProcessInstance(String processId, Map<String, Object> parameters) {
		return createProcessInstance(processId, null, parameters);
	}

	public ProcessInstance startProcessInstance(String processInstanceId, String trigger) {

		ProcessInstance processInstance = getProcessInstance(processInstanceId);
		((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance).configureSLA();
		getProcessEventSupport().fireBeforeProcessStarted(processInstance, this);
		((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance).start(trigger);
		getProcessEventSupport().fireAfterProcessStarted(processInstance, this);
		return processInstance;
	}

	public ProcessInstance startProcessInstance(String processInstanceId) {
		return startProcessInstance(processInstanceId, null);
	}

	@Override
	public ProcessInstance startProcess(String processId, CorrelationKey correlationKey,
			Map<String, Object> parameters) {
		ProcessInstance processInstance = createProcessInstance(processId, correlationKey, parameters);
		if (processInstance != null) {
			processInstanceManager.addProcessInstance(processInstance, correlationKey);
			return startProcessInstance(processInstance.getId());
		}
		return null;
	}

	@Override
	public ProcessInstance createProcessInstance(String processId, CorrelationKey correlationKey,
			Map<String, Object> parameters) {
		final Process process = runtimeContext.findProcess(processId)
				.orElseThrow(() -> new IllegalArgumentException("Unknown process ID: " + processId));

		return createProcessInstance(process, correlationKey, parameters);
	}

	@Override
	public ProcessInstance getProcessInstance(CorrelationKey correlationKey) {
		return processInstanceManager.getProcessInstance(correlationKey);
	}

	private io.automatik.engine.workflow.base.instance.ProcessInstance createProcessInstance(Process process,
			CorrelationKey correlationKey, Map<String, Object> parameters) {
		io.automatik.engine.workflow.base.instance.ProcessInstance pi = runtimeContext.createProcessInstance(process,
				correlationKey);

		pi.setProcessRuntime(this);
		runtimeContext.setupParameters(pi, parameters);
		return pi;
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
		for (Process process : runtimeContext.getProcesses()) {
			initProcessEventListener(process);
		}
	}

	public void removeProcessEventListeners() {
		for (Process process : runtimeContext.getProcesses()) {
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

	public void addEventListener(final ProcessEventListener listener) {
		this.processEventSupport.addEventListener(listener);
	}

	public void removeEventListener(final ProcessEventListener listener) {
		this.processEventSupport.removeEventListener(listener);
	}

	public List<ProcessEventListener> getProcessEventListeners() {
		return processEventSupport.getEventListeners();
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
			startProcessWithParamsAndTrigger(processId, params, type, false);
		}
	}

	private void startProcessWithParamsAndTrigger(String processId, Map<String, Object> params, String type,
			boolean dispose) {

		startProcess(processId, params, type);
	}

	public void abortProcessInstance(String processInstanceId) {
		ProcessInstance processInstance = getProcessInstance(processInstanceId);
		if (processInstance == null) {
			throw new IllegalArgumentException("Could not find process instance for id " + processInstanceId);
		}
		((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance)
				.setState(ProcessInstance.STATE_ABORTED);
	}

	public WorkItemManager getWorkItemManager() {
		return workItemManager;
	}

	@Override
	public UnitOfWorkManager getUnitOfWorkManager() {
		return this.unitOfWorkManager;
	}

	public void signalEvent(String type, Object event) {
		signalManager.signalEvent(type, event);
	}

	public void signalEvent(String type, Object event, String processInstanceId) {
		signalManager.signalEvent(processInstanceId, type, event);
	}

	public void setProcessEventSupport(ProcessEventSupport processEventSupport) {
		this.processEventSupport = processEventSupport;
	}

	public void dispose() {
		this.processEventSupport.reset();
		runtimeContext = null;
	}

	public void clearProcessInstances() {
		this.processInstanceManager.clearProcessInstances();
	}

	public void clearProcessInstancesState() {
		this.processInstanceManager.clearProcessInstancesState();
	}

	public boolean isActive() {
		// originally: kruntime.getEnvironment().get("Active");
		return runtimeContext.isActive();
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
				if (parsedReapedCount > -1) {
					parsedReapedCount = Integer.MAX_VALUE;
				}
				return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2]);
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
			return DurationExpirationTime.repeat(duration);

		case Timer.TIME_DATE:

			return ExactExpirationTime.of(timer.getDate());

		default:
			throw new UnsupportedOperationException("Not supported timer definition");
		}

	}

	@Override
	public Process getProcess(String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
