package io.automatiko.engine.workflow.util;

public class InstanceTuple {

    private final String id;

    private final int status;

    public InstanceTuple(String id, int status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "InstanceTuple [id=" + id + ", status=" + status + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InstanceTuple other = (InstanceTuple) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
