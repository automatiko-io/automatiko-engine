
package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 510l;

	private String id;
	private String type;
	private String name;

	public Message(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
