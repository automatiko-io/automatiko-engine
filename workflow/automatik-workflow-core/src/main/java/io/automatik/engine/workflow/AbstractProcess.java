
package io.automatik.engine.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.ExactExpirationTime;
import io.automatik.engine.api.jobs.ExpirationTime;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.api.runtime.process.EventListener;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstances;
import io.automatik.engine.api.workflow.ProcessInstancesFactory;
import io.automatik.engine.api.workflow.Signal;
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

	protected AbstractProcess() {
		this(new LightProcessRuntimeServiceProvider());
	}

	protected AbstractProcess(ProcessConfig config) {
		this(new ConfiguredProcessServices(config));
	}

	protected AbstractProcess(ProcessRuntimeServiceProvider services) {
		this.services = services;
		this.instances = new MapProcessInstances<>();
	}

	@Override
	public String id() {
		return process().getId();
	}

	public String name() {
		return process().getName();
	}

	@Override
	public T createModel() {
		return null;
	}

	@Override
	public ProcessInstance<T> createInstance(String businessKey, Model m) {
		return createInstance(businessKey, m);
	}

	public abstract ProcessInstance<? extends Model> createInstance(WorkflowProcessInstance wpi);

	@Override
	public ProcessInstances<T> instances() {
		return instances;
	}

	@Override
	public <S> void send(Signal<S> signal) {
		instances().values().forEach(pi -> pi.send(signal));
	}

	public Process<T> configure() {

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
