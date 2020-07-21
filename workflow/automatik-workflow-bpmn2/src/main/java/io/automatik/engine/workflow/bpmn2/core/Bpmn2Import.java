package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Bpmn2Import implements Serializable {

	private static final long serialVersionUID = 6625038042886559671L;

	private String type;
	private String location;
	private String namespace;

	public Bpmn2Import(String type, String location, String namespace) {
		super();
		this.type = type;
		this.location = location;
		this.namespace = namespace;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

}
