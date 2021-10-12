package io.automatiko.addons.fault.tolerance;

public class CircuitBrakerDTO {

    private String name;

    private long count;

    public CircuitBrakerDTO() {

    }

    public CircuitBrakerDTO(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "CircuitBrakerInfo [name=" + name + ", count=" + count + "]";
    }

}
