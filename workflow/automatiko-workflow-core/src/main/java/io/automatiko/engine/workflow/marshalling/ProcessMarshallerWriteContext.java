package io.automatiko.engine.workflow.marshalling;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategyStore;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.marshalling.impl.MarshallerWriteContext;

public class ProcessMarshallerWriteContext extends MarshallerWriteContext {

	public static final int STATE_ACTIVE = 1;
	public static final int STATE_COMPLETED = 2;

	private String processInstanceId;
	private String taskId;
	private String workItemId;
	private int state;

	public ProcessMarshallerWriteContext(OutputStream stream, InternalProcessRuntime processRuntime,
			ObjectMarshallingStrategyStore resolverStrategyFactory, Map<String, Object> env) throws IOException {
		super(stream, processRuntime, resolverStrategyFactory, env);
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getWorkItemId() {
		return workItemId;
	}

	public void setWorkItemId(String workItemId) {
		this.workItemId = workItemId;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

}
