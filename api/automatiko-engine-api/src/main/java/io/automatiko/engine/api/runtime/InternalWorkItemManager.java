package io.automatiko.engine.api.runtime;

import java.util.Map;
import java.util.Set;

import io.automatiko.engine.api.runtime.process.WorkItem;

public interface InternalWorkItemManager extends io.automatiko.engine.api.runtime.process.WorkItemManager {

	void internalExecuteWorkItem(WorkItem workItem);

	void internalAddWorkItem(WorkItem workItem);

	void internalAbortWorkItem(String id);

	void internalCompleteWorkItem(WorkItem workItem);

	Set<WorkItem> getWorkItems();

	WorkItem getWorkItem(String id);

	void clear();

	public void signalEvent(String type, Object event);

	public void signalEvent(String type, Object event, String processInstanceId);

	void dispose();

	void retryWorkItem(String workItemID, Map<String, Object> params);

}
