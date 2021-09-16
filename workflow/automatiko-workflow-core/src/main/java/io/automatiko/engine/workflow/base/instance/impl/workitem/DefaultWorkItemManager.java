package io.automatiko.engine.workflow.base.instance.impl.workitem;

import static io.automatiko.engine.api.runtime.process.WorkItem.ABORTED;
import static io.automatiko.engine.api.runtime.process.WorkItem.COMPLETED;
import static io.automatiko.engine.api.runtime.process.WorkItem.FAILED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.automatiko.engine.api.runtime.Closeable;
import io.automatiko.engine.api.runtime.InternalWorkItemManager;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;

public class DefaultWorkItemManager implements InternalWorkItemManager {

    private Map<String, WorkItem> workItems = new ConcurrentHashMap<>();
    private InternalProcessRuntime runtime;
    private Map<String, WorkItemHandler> workItemHandlers = new HashMap<>();

    public DefaultWorkItemManager(InternalProcessRuntime runtime) {
        this.runtime = runtime;
    }

    public void internalExecuteWorkItem(WorkItem workItem) {
        ((WorkItemImpl) workItem).setId(UUID.randomUUID().toString());
        internalAddWorkItem(workItem);
        WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
        if (handler != null) {
            handler.executeWorkItem(workItem, this);
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
            WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
            if (handler != null) {
                handler.abortWorkItem(workItem, this);
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
        WorkItem workItem = workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {
            ((WorkItemImpl) workItem).setResults(results);
            ProcessInstance processInstance = runtime.getProcessInstance(workItem.getProcessInstanceId());
            ((WorkItemImpl) workItem).setState(COMPLETED);
            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemCompleted", workItem);
            }
            workItems.remove(id);
        }
    }

    public void abortWorkItem(String id, Policy<?>... policies) {
        WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {
            ProcessInstance processInstance = runtime.getProcessInstance(workItem.getProcessInstanceId());
            workItem.setState(ABORTED);
            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemAborted", workItem);
            }
            workItems.remove(id);
        }
    }

    @Override
    public void failWorkItem(String id, Throwable error) {
        WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
        // work item may have been aborted
        if (workItem != null) {
            ProcessInstance processInstance = runtime.getProcessInstance(workItem.getProcessInstanceId());
            workItem.setState(FAILED);
            // process instance may have finished already
            if (processInstance != null) {
                workItem.setResult("Error", error);
                processInstance.signalEvent("workItemFailed", workItem);
            }
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
        this.runtime.signalEvent(type, event);
    }

    public void signalEvent(String type, Object event, String processInstanceId) {
        this.runtime.signalEvent(type, event, processInstanceId);
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

    @Override
    public void internalCompleteWorkItem(WorkItem workItem) {

    }

    public void internalAbortWorkItem(WorkItem workItem) {

    }

    public void internalRemoveWorkItem(WorkItem workItem) {
        this.workItems.remove(workItem.getId());

    }
}
