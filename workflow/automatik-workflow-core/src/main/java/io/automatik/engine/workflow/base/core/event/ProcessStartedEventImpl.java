package io.automatik.engine.workflow.base.core.event;

import io.automatik.engine.api.event.process.ProcessStartedEvent;
import io.automatik.engine.api.runtime.process.ProcessInstance;
import io.automatik.engine.api.runtime.process.ProcessRuntime;

public class ProcessStartedEventImpl extends ProcessEvent implements ProcessStartedEvent {

	private static final long serialVersionUID = 510l;

	public ProcessStartedEventImpl(final ProcessInstance instance, ProcessRuntime runtime) {
		super(instance, runtime);
	}

	public String toString() {
		return "==>[ProcessStarted(name=" + getProcessInstance().getProcessName() + "; id="
				+ getProcessInstance().getProcessId() + ")]";
	}

}
