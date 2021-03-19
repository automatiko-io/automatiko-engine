package io.automatiko.engine.workflow.base.core.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.automatiko.engine.api.event.process.DelayedExecution;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatiko.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.event.process.ProcessStartedEvent;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.api.event.process.ProcessWorkItemTransitionEvent;
import io.automatiko.engine.api.event.process.SLAViolatedEvent;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.workitem.Transition;

public class ProcessEventSupport extends AbstractEventSupport<ProcessEventListener> {

    private UnitOfWorkManager unitOfWorkManager;

    public ProcessEventSupport(UnitOfWorkManager unitOfWorkManager) {
        this.unitOfWorkManager = unitOfWorkManager;
    }

    public ProcessEventSupport() {
    }

    public void fireBeforeProcessStarted(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessStartedEvent event = new ProcessStartedEventImpl(instance, runtime);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeProcessStarted(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {

            delayedListeners.forEach(l -> l.beforeProcessStarted(e));
        }));
    }

    public void fireAfterProcessStarted(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessStartedEvent event = new ProcessStartedEventImpl(instance, runtime);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterProcessStarted(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterProcessStarted(e));
        }));
    }

    public void fireBeforeProcessCompleted(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessCompletedEvent event = new ProcessCompletedEventImpl(instance, runtime);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeProcessCompleted(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeProcessCompleted(e));
        }));
    }

    public void fireAfterProcessCompleted(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessCompletedEvent event = new ProcessCompletedEventImpl(instance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterProcessCompleted(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterProcessCompleted(e));
        }));
    }

    public void fireBeforeNodeTriggered(final NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessNodeTriggeredEvent event = new ProcessNodeTriggeredEventImpl(nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeNodeTriggered(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeNodeTriggered(e));
        }));
    }

    public void fireAfterNodeTriggered(final NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessNodeTriggeredEvent event = new ProcessNodeTriggeredEventImpl(nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterNodeTriggered(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterNodeTriggered(e));
        }));
    }

    public void fireBeforeNodeLeft(final NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessNodeLeftEvent event = new ProcessNodeLeftEventImpl(nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeNodeLeft(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeNodeLeft(e));
        }));
    }

    public void fireAfterNodeLeft(final NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessNodeLeftEvent event = new ProcessNodeLeftEventImpl(nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterNodeLeft(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterNodeLeft(e));
        }));
    }

    public void fireBeforeVariableChanged(final String id, final String instanceId, final Object oldValue,
            final Object newValue, final List<String> tags, final ProcessInstance processInstance,
            NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessVariableChangedEvent event = new ProcessVariableChangedEventImpl(id, instanceId, oldValue,
                newValue, tags, processInstance, nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeVariableChanged(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeVariableChanged(e));
        }));
    }

    public void fireAfterVariableChanged(final String name, final String id, final Object oldValue,
            final Object newValue, final List<String> tags, final ProcessInstance processInstance,
            NodeInstance nodeInstance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessVariableChangedEvent event = new ProcessVariableChangedEventImpl(name, id, oldValue, newValue,
                tags, processInstance, nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterVariableChanged(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterVariableChanged(e));
        }));
    }

    public void fireBeforeSLAViolated(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeSLAViolated(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeSLAViolated(e));
        }));
    }

    public void fireAfterSLAViolated(final ProcessInstance instance, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterSLAViolated(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterSLAViolated(e));
        }));
    }

    public void fireBeforeSLAViolated(final ProcessInstance instance, NodeInstance nodeInstance,
            ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, nodeInstance, runtime);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeSLAViolated(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.beforeSLAViolated(e));
        }));
    }

    public void fireAfterSLAViolated(final ProcessInstance instance, NodeInstance nodeInstance,
            ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final SLAViolatedEvent event = new SLAViolatedEventImpl(instance, nodeInstance, runtime);
        if (iter.hasNext()) {

            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterSLAViolated(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, e -> {
            delayedListeners.forEach(l -> l.afterSLAViolated(e));
        }));
    }

    public void fireBeforeWorkItemTransition(final ProcessInstance instance, WorkItem workitem,
            Transition<?> transition, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessWorkItemTransitionEvent event = new ProcessWorkItemTransitionEventImpl(instance, workitem,
                transition, runtime, false);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.beforeWorkItemTransition(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, (e) -> {
            delayedListeners.forEach(l -> l.beforeWorkItemTransition(e));
        }));
    }

    public void fireAfterWorkItemTransition(final ProcessInstance instance, WorkItem workitem, Transition<?> transition,
            ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessWorkItemTransitionEvent event = new ProcessWorkItemTransitionEventImpl(instance, workitem,
                transition, runtime, true);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterWorkItemTransition(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, (e) -> {
            delayedListeners.forEach(l -> l.afterWorkItemTransition(e));
        }));
    }

    public void fireAfterNodeInstanceFailed(final ProcessInstance instance, NodeInstance nodeInstance,
            String errorId, String errorMessage, Exception exception, ProcessRuntime runtime) {
        final Iterator<ProcessEventListener> iter = getEventListenersIterator();
        final List<ProcessEventListener> delayedListeners = new ArrayList<ProcessEventListener>();
        final ProcessNodeInstanceFailedEvent event = new ProcessNodeInstanceFailedEventImpl(instance, nodeInstance,
                errorId, errorMessage, exception, runtime);
        if (iter.hasNext()) {
            do {
                ProcessEventListener listener = iter.next();
                if (listener instanceof DelayedExecution) {
                    delayedListeners.add(listener);
                } else {
                    listener.afterNodeInstanceFailed(event);
                }
            } while (iter.hasNext());
        }
        unitOfWorkManager.currentUnitOfWork().intercept(WorkUnit.create(event, (e) -> {
            delayedListeners.forEach(l -> l.afterNodeInstanceFailed(e));
        }));
    }

    public void reset() {
        this.clear();
    }
}
