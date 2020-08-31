
package io.automatik.engine.workflow.base.instance.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.services.correlation.CorrelationKey;
import io.automatik.engine.workflow.base.instance.ProcessInstanceManager;
import io.automatik.engine.workflow.process.instance.impl.WorkflowProcessInstanceImpl;

public class DefaultProcessInstanceManager implements ProcessInstanceManager {

	private Map<String, ProcessInstance> processInstances = new ConcurrentHashMap<>();
	private Map<CorrelationKey, ProcessInstance> processInstancesByCorrelationKey = new ConcurrentHashMap<>();

	public void addProcessInstance(ProcessInstance processInstance, CorrelationKey correlationKey) {
		String uuid;
		if (correlationKey != null) {
			uuid = UUID.nameUUIDFromBytes(correlationKey.toExternalForm().getBytes()).toString();
			if (processInstancesByCorrelationKey.containsKey(correlationKey)) {
				throw new RuntimeException(correlationKey + " already exists");
			}
			processInstancesByCorrelationKey.put(correlationKey, processInstance);
			((WorkflowProcessInstanceImpl) processInstance).setCorrelationKey(correlationKey.toExternalForm());
		} else {
			uuid = UUID.randomUUID().toString();
		}

		((io.automatik.engine.workflow.base.instance.ProcessInstance) processInstance).setId(uuid);
		internalAddProcessInstance(processInstance);
	}

	public void internalAddProcessInstance(ProcessInstance processInstance) {
		processInstances.put(processInstance.getId(), processInstance);
	}

	public Collection<ProcessInstance> getProcessInstances() {
		return Collections.unmodifiableCollection(processInstances.values());
	}

	public ProcessInstance getProcessInstance(String id) {
		return processInstances.get(id);
	}

	public ProcessInstance getProcessInstance(String id, boolean readOnly) {
		return processInstances.get(id);
	}

	public void removeProcessInstance(ProcessInstance processInstance) {
		internalRemoveProcessInstance(processInstance);
	}

	public void internalRemoveProcessInstance(ProcessInstance processInstance) {
		processInstances.remove(processInstance.getId());
		for (Entry<CorrelationKey, ProcessInstance> entry : processInstancesByCorrelationKey.entrySet()) {
			if (entry.getValue().getId().equals(processInstance.getId())) {
				processInstancesByCorrelationKey.remove(entry.getKey());
			}
		}
	}

	public void clearProcessInstances() {
		processInstances.clear();
	}

	public void clearProcessInstancesState() {

	}

	@Override
	public ProcessInstance getProcessInstance(CorrelationKey correlationKey) {
		return processInstancesByCorrelationKey.get(correlationKey);
	}
}
