package io.automatiko.addons.fault.tolerance;

public class CircuitClosedEvent {

    private final String name;

    public CircuitClosedEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CircuitOpenedEvent [name=" + name + "]";
    }

}
