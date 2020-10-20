package io.automatik.engine.workflow.base.instance;

import io.automatik.engine.api.workflow.Tag;

public class TagInstance implements Tag {

    private String id;

    private String value;

    public TagInstance() {

    }

    public TagInstance(String id, String value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Tag [id=" + id + ", value=" + value + "]";
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
        TagInstance other = (TagInstance) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
