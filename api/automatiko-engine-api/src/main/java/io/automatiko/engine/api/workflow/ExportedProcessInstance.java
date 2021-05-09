package io.automatiko.engine.api.workflow;

import java.util.List;
import java.util.Map;

public abstract class ExportedProcessInstance<T> {

    private final T header;

    private final T instance;

    private final T timers;

    protected ExportedProcessInstance(T header, T instance, T timers) {
        this.header = header;
        this.instance = instance;
        this.timers = timers;
    }

    public T getHeader() {
        return header;
    }

    public T getInstance() {
        return instance;
    }

    public T getTimers() {
        return timers;
    }

    public abstract List<Map<String, String>> convertTimers();

}
