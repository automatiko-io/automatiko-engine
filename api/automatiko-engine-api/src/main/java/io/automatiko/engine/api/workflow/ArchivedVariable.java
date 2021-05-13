package io.automatiko.engine.api.workflow;

public abstract class ArchivedVariable {

    protected final String name;

    protected final Object value;

    public ArchivedVariable(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public abstract byte[] data();
}
