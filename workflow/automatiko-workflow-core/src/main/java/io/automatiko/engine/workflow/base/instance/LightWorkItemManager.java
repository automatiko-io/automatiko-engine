
package io.automatiko.engine.workflow.base.instance;

import static io.automatiko.engine.api.runtime.process.WorkItem.ABORTED;
import static io.automatiko.engine.api.runtime.process.WorkItem.COMPLETED;
import static io.automatiko.engine.workflow.base.instance.impl.workitem.Abort.ID;
import static io.automatiko.engine.workflow.base.instance.impl.workitem.Abort.STATUS;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.automatiko.engine.api.runtime.Closeable;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemNotFoundException;
import io.automatiko.engine.api.workflow.signal.SignalManager;
import io.automatiko.engine.api.workflow.workitem.NotAuthorizedException;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.workflow.base.core.event.ProcessEventSupport;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Abort;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Complete;
import io.automatiko.engine.workflow.base.instance.impl.workitem.DefaultWorkItemManager;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemHandlerNotFoundException;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;

public class LightWorkItemManager extends DefaultWorkItemManager {

    private Map<String, WorkItem> workItems = new ConcurrentHashMap<>();
    private Map<String, WorkItemHandler> workItemHandlers = new HashMap<>();

    private final ProcessInstanceManager processInstanceManager;
    private final SignalManager signalManager;
    private final ProcessEventSupport eventSupport;

    private Complete completePhase = new Complete();
    private Abort abortPhase = new Abort();

    public LightWorkItemManager(InternalProcessRuntime runtime, ProcessInstanceManager processInstanceManager,
            SignalManager signalManager, ProcessEventSupport eventSupport) {
        super(runtime);
        this.processInstanceManager = processInstanceManager;
        this.signalManager = signalManager;
        this.eventSupport = eventSupport;
    }

    public void internalExecuteWorkItem(WorkItem workItem) {
        ((WorkItemImpl) workItem).setId(UUID.randomUUID().toString());
        internalAddWorkItem(workItem);
        WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
        if (handler != null) {
            ProcessInstance processInstance = workItem.getProcessInstance();
            if (processInstance == null) {
                processInstance = processInstanceManager.getProcessInstance(workItem.getProcessInstanceId());
            }
            Transition<?> transition = new TransitionToActive();
            eventSupport.fireBeforeWorkItemTransition(processInstance, workItem, transition, null);

            handler.executeWorkItem(workItem, this);

            eventSupport.fireAfterWorkItemTransition(processInstance, workItem, transition, null);
        } else
            throw new WorkItemHandlerNotFoundException(workItem.getName());
    }

    public void internalAddWorkItem(WorkItem workItem) {
        workItems.put(workItem.getId(), workItem);
    }

    public void internalAbortWorkItem(String id) {
        WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {
            workItem.setCompleteDate(new Date());
            WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
            if (handler != null) {

                ProcessInstance processInstance = workItem.getProcessInstance();
                if (processInstance == null) {
                    processInstance = processInstanceManager.getProcessInstance(workItem.getProcessInstanceId());
                }
                Transition<?> transition = new TransitionToAbort(Collections.emptyList());
                eventSupport.fireBeforeWorkItemTransition(processInstance, workItem, transition, null);

                handler.abortWorkItem(workItem, this);
                workItem.setPhaseId(ID);
                workItem.setPhaseStatus(STATUS);
                eventSupport.fireAfterWorkItemTransition(processInstance, workItem, transition, null);
            } else {
                workItems.remove(workItem.getId());
                throw new WorkItemHandlerNotFoundException(workItem.getName());
            }
            workItems.remove(workItem.getId());
        }
    }

    public void retryWorkItem(String workItemId) {
        WorkItem workItem = workItems.get(workItemId);
        retryWorkItem(workItem);
    }

    public void retryWorkItemWithParams(String workItemId, Map<String, Object> map) {
        WorkItem workItem = workItems.get(workItemId);

        if (workItem != null) {
            ((WorkItemImpl) workItem).setParameters(map);

            retryWorkItem(workItem);
        }
    }

    private void retryWorkItem(WorkItem workItem) {
        if (workItem != null) {
            WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
            if (handler != null) {
                handler.executeWorkItem(workItem, this);
            } else
                throw new WorkItemHandlerNotFoundException(workItem.getName());
        }
    }

    public Set<WorkItem> getWorkItems() {
        return new HashSet<>(workItems.values());
    }

    public WorkItem getWorkItem(String id) {
        return workItems.get(id);
    }

    public void completeWorkItem(String id, Map<String, Object> results, Policy<?>... policies) {
        transitionWorkItem(id, new TransitionToComplete(results, Arrays.asList(policies)));
    }

    public void internalCompleteWorkItem(WorkItem workItem) {
        ProcessInstance processInstance = workItem.getProcessInstance();
        if (processInstance == null) {
            processInstance = processInstanceManager.getProcessInstance(workItem.getProcessInstanceId());
        }
        ((WorkItemImpl) workItem).setState(COMPLETED);
        workItem.setCompleteDate(new Date());

        // process instance may have finished already
        if (processInstance != null) {
            processInstance.signalEvent("workItemCompleted", workItem);
        }
        workItems.remove(workItem.getId());

    }

    public void internalAbortWorkItem(WorkItem workItem) {
        ProcessInstance processInstance = workItem.getProcessInstance();
        if (processInstance == null) {
            processInstance = processInstanceManager.getProcessInstance(workItem.getProcessInstanceId());
        }

        ((WorkItemImpl) workItem).setState(ABORTED);

        // process instance may have finished already
        if (processInstance != null) {
            processInstance.signalEvent("workItemAborted", workItem);
        }
        workItems.remove(workItem.getId());

    }

    @SuppressWarnings("unchecked")
    @Override
    public void transitionWorkItem(String id, Transition<?> transition) {
        WorkItem workItem = workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {

            WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
            if (handler != null) {
                ProcessInstance processInstance = workItem.getProcessInstance();
                if (processInstance == null) {
                    processInstance = processInstanceManager.getProcessInstance(workItem.getProcessInstanceId());
                }
                eventSupport.fireBeforeWorkItemTransition(processInstance, workItem, transition, null);

                try {
                    handler.transitionToPhase(workItem, this, transition);
                } catch (UnsupportedOperationException e) {
                    ((WorkItemImpl) workItem).setResults((Map<String, Object>) transition.data());
                    workItem.setPhaseId(Complete.ID);
                    workItem.setPhaseStatus(Complete.STATUS);
                    completePhase.apply(workItem, transition);
                    internalCompleteWorkItem(workItem);
                }

                eventSupport.fireAfterWorkItemTransition(processInstance, workItem, transition, null);
            } else {
                throw new WorkItemHandlerNotFoundException(workItem.getName());
            }

        } else {
            throw new WorkItemNotFoundException("Work Item (" + id + ") does not exist", id);
        }
    }

    public void abortWorkItem(String id, Policy<?>... policies) {
        WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {
            if (!workItem.enforce(policies)) {
                throw new NotAuthorizedException(
                        "Work item can be aborted as it does not fulfil policies (e.g. security)");
            }
            ProcessInstance processInstance = processInstanceManager
                    .getProcessInstance(workItem.getProcessInstanceId());
            Transition<?> transition = new TransitionToAbort(Arrays.asList(policies));
            eventSupport.fireBeforeWorkItemTransition(processInstance, workItem, transition, null);
            workItem.setState(ABORTED);
            abortPhase.apply(workItem, transition);

            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemAborted", workItem);
            }
            workItem.setPhaseId(ID);
            workItem.setPhaseStatus(STATUS);
            eventSupport.fireAfterWorkItemTransition(processInstance, workItem, transition, null);
            workItems.remove(id);
        }
    }

    public void registerWorkItemHandler(String workItemName, WorkItemHandler handler) {
        this.workItemHandlers.put(workItemName, handler);
    }

    public void clear() {
        this.workItems.clear();
    }

    public void signalEvent(String type, Object event) {
        this.signalManager.signalEvent(type, event);
    }

    public void signalEvent(String type, Object event, String processInstanceId) {
        this.signalManager.signalEvent(processInstanceId, type, event);
    }

    @Override
    public void dispose() {
        if (workItemHandlers != null) {
            for (Map.Entry<String, WorkItemHandler> handlerEntry : workItemHandlers.entrySet()) {
                if (handlerEntry.getValue() instanceof Closeable) {
                    ((Closeable) handlerEntry.getValue()).close();
                }
            }
        }
    }

    @Override
    public void retryWorkItem(String workItemID, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            retryWorkItem(workItemID);
        } else {
            this.retryWorkItemWithParams(workItemID, params);
        }

    }

    public WorkItemHandler getWorkItemHandler(String name) {
        return this.workItemHandlers.get(name);
    }

    private static class TransitionToActive implements Transition<Void> {

        @Override
        public String phase() {
            return Active.ID;
        }

        @Override
        public Void data() {
            return null;
        }

        @Override
        public List<Policy<?>> policies() {
            return Collections.emptyList();
        }
    }

    private static class TransitionToAbort implements Transition<Void> {

        private List<Policy<?>> policies;

        TransitionToAbort(List<Policy<?>> policies) {
            this.policies = policies;
        }

        @Override
        public String phase() {
            return ID;
        }

        @Override
        public Void data() {
            return null;
        }

        @Override
        public List<Policy<?>> policies() {
            return policies;
        }
    }

    private static class TransitionToComplete implements Transition<Map<String, Object>> {
        private Map<String, Object> data;
        private List<Policy<?>> policies;

        TransitionToComplete(Map<String, Object> data, List<Policy<?>> policies) {
            this.data = data;
            this.policies = policies;
        }

        @Override
        public String phase() {
            return Complete.ID;
        }

        @Override
        public Map<String, Object> data() {
            return data;
        }

        @Override
        public List<Policy<?>> policies() {
            return policies;
        }
    }
}
