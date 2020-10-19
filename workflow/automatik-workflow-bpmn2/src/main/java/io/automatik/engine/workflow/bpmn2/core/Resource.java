package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Resource implements Serializable {

    private static final long serialVersionUID = 510l;

    private String id;
    private String name;

    public Resource(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
