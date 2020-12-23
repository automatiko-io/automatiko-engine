package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Association implements Serializable {

    private static final long serialVersionUID = 4L;

    private String id;
    private String sourceRef;
    private String targetRef;
    private String direction = "none";

    private Map<String, Object> metaData = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String name, Object value) {
        this.metaData.put(name, value);
    }

    public Object getMetaData(String name) {
        return this.metaData.get(name);
    }

    public String toString() {
        return "Association (" + this.id + ") [" + this.sourceRef + " -> " + this.targetRef + "]";
    }
}
