package io.automatik.engine.workflow.base.core.event;

import java.util.Iterator;
import java.util.List;

import io.automatik.engine.api.event.process.ProcessCompletedEvent;
import io.automatik.engine.api.event.process.ProcessEventListener;
import io.automatik.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatik.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatik.engine.api.event.process.ProcessStartedEvent;
import io.automatik.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatik.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatik.engine.api.event.process.SLAViolatedEvent;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.uow.UnitOfWorkManager;
import io.automatik.engine.api.uow.WorkUnit;
import io.automatik.engine.api.workflow.workitem.Transition;

public class ProcessEventSupport extends AbstractEventSupport<ProcessEventListener> {

	private UnitOfWorkManager unitOfWorkManager;

	public ProcessEventSupport(UnitOfWorkManager unitOfWorkManager) {
		this.unitOfWorkManager = unitOfWorkManager;
	}

	public ProcessEventSupport() {
	}

	public void fireBeforeProcessStarted(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessStartedEvent event = new ProcessStartedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {
				do {
					iter.next().beforeProcessStarted(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterProcessStarted(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessStartedEvent event = new ProcessStartedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {
				do {
					iter.next().afterProcessStarted(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeProcessCompleted(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessCompletedEvent event = new ProcessCompletedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {
				do {
					iter.next().beforeProcessCompleted(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterProcessCompleted(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessCompletedEvent event = new ProcessCompletedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterProcessCompleted(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeNodeTriggered(final NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessNodeTriggeredEvent event = new ProcessNodeTriggeredEventImpl(nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().beforeNodeTriggered(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterNodeTriggered(final NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessNodeTriggeredEvent event = new ProcessNodeTriggeredEventImpl(nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterNodeTriggered(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeNodeLeft(final NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessNodeLeftEvent event = new ProcessNodeLeftEventImpl(nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().beforeNodeLeft(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterNodeLeft(final NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessNodeLeftEvent event = new ProcessNodeLeftEventImpl(nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterNodeLeft(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeVariableChanged(final String id, final String instanceId, final Object oldValue,
			final Object newValue, final List<String> tags, final ProcessInstance processInstance,
			NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessVariableChangedEvent event = new ProcessVariableChangedEventImpl(id, instanceId, oldValue,
				newValue, tags, processInstance, nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().beforeVariableChanged(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterVariableChanged(final String name, final String id, final Object oldValue,
			final Object newValue, final List<String> tags, final ProcessInstance processInstance,
			NodeInstance nodeInstance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();
		final ProcessVariableChangedEvent event = new ProcessVariableChangedEventImpl(name, id, oldValue, newValue,
				tags, processInstance, nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterVariableChanged(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeSLAViolated(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().beforeSLAViolated(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterSLAViolated(final ProcessInstance instance, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterSLAViolated(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeSLAViolated(final ProcessInstance instance, NodeInstance nodeInstance,
			ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {
				do {
					iter.next().beforeSLAViolated(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterSLAViolated(final ProcessInstance instance, NodeInstance nodeInstance,
			ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();
		final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, nodeInstance, runtime);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
			if (iter.hasNext()) {

				do {
					iter.next().afterSLAViolated(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireBeforeWorkItemTransition(final ProcessInstance instance, WorkItem workitem,
			Transition<?> transition, ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessWorkItemTransitionEvent event = new ProcessWorkItemTransitionEventImpl(instance, workitem,
				transition, runtime, false);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, (e) -> {
			if (iter.hasNext()) {
				do {
					iter.next().beforeWorkItemTransition(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void fireAfterWorkItemTransition(final ProcessInstance instance, WorkItem workitem, Transition<?> transition,
			ProcessRuntime runtime) {
		final Iterator<ProcessEventListener> iter = getEventListenersIterator();

		final ProcessWorkItemTransitionEvent event = new ProcessWorkItemTransitionEventImpl(instance, workitem,
				transition, runtime, true);
		unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, (e) -> {
			if (iter.hasNext()) {
				do {
					iter.next().afterWorkItemTransition(e);
				} while (iter.hasNext());
			}
		}));
	}

	public void reset() {
		this.clear();
	}
}
