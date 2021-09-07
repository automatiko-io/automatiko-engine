package io.automatiko.engine.workflow.base.core.event;

import io.automatiko.engine.api.event.process.ProcessSignaledEvent;
import io.automatiko.engine.api.runtime.process.ProcessInstance;
import io.automatiko.engine.api.runtime.process.ProcessRuntime;

public class ProcessSignaledEventImpl extends ProcessEvent implements ProcessSignaledEvent {

    private static final long serialVersionUID = 510l;
    private String signal;
    private Object data;

    public ProcessSignaledEventImpl(final ProcessInstance instance, ProcessRuntime runtime) {
        super(instance, runtime);
    }

    public ProcessSignaledEventImpl(String signal, Object data, final ProcessInstance instance, ProcessRuntime runtime) {
        super(instance, runtime);
        this.signal = signal;
        this.data = data;
    }

    public String toString() {
        return "==>[ProcessSignaledEventImpl(name=" + getProcessInstance().getProcessName() + "; id="
                + getProcessInstance().getProcessId() + "; signal=" + signal + ")]";
    }

    @Override
    public String getSignal() {
        return signal;
    }

    @Override
    public Object getData() {
        return data;
    }

}
