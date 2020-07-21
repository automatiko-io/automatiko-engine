package io.automatik.engine.workflow.base.core.event;

import java.util.Collections;
import java.util.List;

import io.automatik.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatik.engine.api.runtime.process.NodeInstance;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class ProcessVariableChangedEventImpl extends ProcessEvent implements ProcessVariableChangedEvent {

	private static final long serialVersionUID = 510l;

	private String id;
	private String instanceId;
	private Object oldValue;
	private Object newValue;
	private List<String> tags;

	private NodeInstance nodeInstance;

	public ProcessVariableChangedEventImpl(final String id, final String instanceId, final Object oldValue,
			final Object newValue, List<String> tags, final ProcessInstance processInstance, NodeInstance nodeInstance,
			ProcessRuntime runtime) {
		super(processInstance, runtime);
		this.nodeInstance = nodeInstance;
		this.id = id;
		this.instanceId = instanceId;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.tags = tags == null ? Collections.emptyList() : tags;
	}

	public String getVariableInstanceId() {
		return instanceId;
	}

	public String getVariableId() {
		return id;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public List<String> getTags() {
		return tags;
	}

	@Override
	public NodeInstance getNodeInstance() {
		return this.nodeInstance;
	}

	public String toString() {
		return "==>[ProcessVariableChanged(id=" + id + "; instanceId=" + instanceId + "; oldValue=" + oldValue
				+ "; newValue=" + newValue + "; processName=" + getProcessInstance().getProcessName() + "; processId="
				+ getProcessInstance().getProcessId() + ")]";
	}

}
