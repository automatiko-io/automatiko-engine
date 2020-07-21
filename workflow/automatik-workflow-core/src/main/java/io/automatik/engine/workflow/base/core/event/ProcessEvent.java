package io.automatik.engine.workflow.base.core.event;

import java.util.Date;
import java.util.EventObject;

import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class ProcessEvent extends EventObject {

	private static final long serialVersionUID = 510l;

	private ProcessRuntime runtime;
	private final Date eventDate;

	public ProcessEvent(final ProcessInstance instance, ProcessRuntime runtime) {
		super(instance);
		this.runtime = runtime;
		this.eventDate = new Date();
	}

	public ProcessInstance getProcessInstance() {
		return (ProcessInstance) getSource();
	}

	public ProcessRuntime getProcessRuntime() {
		return runtime;
	}

	public Date getEventDate() {
		return this.eventDate;
	}

}
