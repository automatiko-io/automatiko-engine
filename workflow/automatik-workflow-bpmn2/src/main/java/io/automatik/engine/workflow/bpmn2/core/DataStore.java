package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;

import io.automatik.engine.api.workflow.datatype.DataType;

public class DataStore implements Serializable {

	private static final long serialVersionUID = 4L;

	private String id;
	private String name;
	private DataType type;
	private String itemSubjectRef;

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(DataType dataType) {
		this.type = dataType;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public DataType getType() {
		return this.type;
	}

	public void setItemSubjectRef(String itemSubjectRef) {
		this.itemSubjectRef = itemSubjectRef;
	}

	public String getItemSubjectRef() {
		return this.itemSubjectRef;
	}

}
